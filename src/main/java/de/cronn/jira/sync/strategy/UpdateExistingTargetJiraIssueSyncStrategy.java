package de.cronn.jira.sync.strategy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.StatusTransitionConfig;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIdResource;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.mapping.DefaultVersionMapper;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.ResolutionMapper;
import de.cronn.jira.sync.resolve.JiraIssueResolver;
import de.cronn.jira.sync.service.JiraService;

@Component
public class UpdateExistingTargetJiraIssueSyncStrategy implements ExistingTargetJiraIssueSyncStrategy {

	private static final Logger log = LoggerFactory.getLogger(UpdateExistingTargetJiraIssueSyncStrategy.class);

	private final JiraIssueResolver jiraIssueResolver;
	private final DescriptionMapper descriptionMapper;
	private final LabelMapper labelMapper;
	private final PriorityMapper priorityMapper;
	private final ResolutionMapper resolutionMapper;

	public UpdateExistingTargetJiraIssueSyncStrategy(JiraIssueResolver jiraIssueResolver, DescriptionMapper descriptionMapper, LabelMapper labelMapper, PriorityMapper priorityMapper, ResolutionMapper resolutionMapper) {
		this.jiraIssueResolver = jiraIssueResolver;
		this.descriptionMapper = descriptionMapper;
		this.labelMapper = labelMapper;
		this.priorityMapper = priorityMapper;
		this.resolutionMapper = resolutionMapper;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		log.debug("sourceIssue={}, targetIssue={}", sourceIssue, targetIssue);

		assertRequiredFieldsArePresent(sourceIssue);
		assertRequiredFieldsArePresent(targetIssue);

		JiraIssueUpdate targetIssueUpdate = new JiraIssueUpdate();
		JiraIssueUpdate sourceIssueUpdate = new JiraIssueUpdate();

		processStatusTransition(jiraSource, sourceIssue, targetIssue, projectSync, sourceIssueUpdate);
		processDescription(sourceIssue, targetIssue, targetIssueUpdate);
		processLabels(sourceIssue, targetIssue, targetIssueUpdate, projectSync);
		processPriority(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate);
		processVersions(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate, projectSync, JiraIssueFields::getVersions, JiraField.VERSIONS);
		processVersions(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate, projectSync, JiraIssueFields::getFixVersions, JiraField.FIX_VERSIONS);

		addBacklinkIfMissing(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		if (sourceIssueUpdate.isEmpty() && targetIssueUpdate.isEmpty()) {
			return SyncResult.UNCHANGED;
		}

		if (!sourceIssueUpdate.isEmpty()) {
			if (sourceIssueUpdate.getTransition() != null) {
				jiraSource.transitionIssue(sourceIssue, sourceIssueUpdate);
			} else {
				log.warn("Ignoring source issue update of {} without transition", sourceIssue);
			}
		}

		if (!targetIssueUpdate.isEmpty()) {
			Assert.isNull(targetIssueUpdate.getTransition());
			jiraTarget.updateIssue(targetIssue, targetIssueUpdate);
		}

		if (sourceIssueUpdate.getTransition() != null) {
			return SyncResult.CHANGED_TRANSITION;
		} else {
			return SyncResult.CHANGED;
		}
	}

	private void addBacklinkIfMissing(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		JiraIssue resolvedIssue = jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource);
		if (resolvedIssue == null) {
			log.warn("Backlink not found in {}", targetIssue);
			jiraTarget.addRemoteLink(targetIssue, sourceIssue, jiraSource, projectSync.getRemoteLinkIconInTarget());
		} else if (!resolvedIssue.getId().equals(sourceIssue.getId())) {
			throw new JiraSyncException("Backlink of " + targetIssue + " points to " + resolvedIssue + "; expected: " + sourceIssue);
		}
	}

	private void processStatusTransition(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync, JiraIssueUpdate sourceIssueUpdate) {
		List<StatusTransitionConfig> statusTransitions = projectSync.getStatusTransitions();
		if (statusTransitions == null) {
			log.trace("No status transitions configured");
			return;
		}

		StatusTransitionConfig transition = findTransition(sourceIssue, targetIssue, statusTransitions);
		if (transition != null) {

			if (transition.isAssignToMyselfInSource()) {
				JiraUser myself = jiraSource.getMyself();
				if (!isEqual(sourceIssue, myself)) {
					JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate();
					jiraIssueUpdate.putFieldUpdate(JiraField.ASSIGNEE, myself);
					jiraSource.updateIssue(sourceIssue, jiraIssueUpdate);
				}
			}

			JiraTransition jiraTransition = getJiraTransition(jiraSource, sourceIssue, transition);
			sourceIssueUpdate.setTransition(jiraTransition);

			if (transition.isCopyResolutionToSource()) {
				processResolution(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate);
			}
		}
	}

	private boolean isEqual(JiraIssue issue, JiraUser jiraUser) {
		JiraUser assignee = issue.getFields().getAssignee();
		if (assignee == null) {
			return false;
		}
		return Objects.equals(jiraUser.getKey(), assignee.getKey());
	}

	private StatusTransitionConfig findTransition(JiraIssue sourceIssue, JiraIssue targetIssue, List<StatusTransitionConfig> statusTransitions) {
		String sourceIssueStatus = getStatusName(sourceIssue);
		String targetIssueStatus = getStatusName(targetIssue);

		List<StatusTransitionConfig> transitionConfigs = statusTransitions.stream()
			.filter(statusTransitionConfig -> statusTransitionConfig.getSourceStatusIn().contains(sourceIssueStatus))
			.filter(statusTransitionConfig -> statusTransitionConfig.getTargetStatusIn().contains(targetIssueStatus))
			.filter(statusTransitionConfig -> filterOnlyIfAssignedInTarget(statusTransitionConfig, targetIssue))
			.collect(Collectors.toList());

		if (transitionConfigs.isEmpty()) {
			return null;
		} else if (transitionConfigs.size() == 1) {
			StatusTransitionConfig transitionConfig = transitionConfigs.get(0);
			String statusToTransitionTo = transitionConfig.getSourceStatusToSet();
			Assert.notNull(statusToTransitionTo);
			return transitionConfig;
		} else {
			throw new JiraSyncException("Illegal number of matching status transitions: " + transitionConfigs);
		}
	}

	private boolean filterOnlyIfAssignedInTarget(StatusTransitionConfig statusTransitionConfig, JiraIssue targetIssue) {
		if (statusTransitionConfig.isOnlyIfAssignedInTarget()) {
			return targetIssue.getFields().getAssignee() != null;
		} else {
			return true;
		}
	}

	private JiraTransition getJiraTransition(JiraService jiraTarget, JiraIssue targetIssue, StatusTransitionConfig transitionConfig) {
		String sourceStatusToSet = transitionConfig.getSourceStatusToSet();
		List<JiraTransition> allTransitions = jiraTarget.getTransitions(targetIssue);
		List<JiraTransition> filteredTransitions = allTransitions.stream()
			.filter(jiraTransition -> jiraTransition.getTo().getName().equals(sourceStatusToSet))
			.collect(Collectors.toList());

		if (filteredTransitions.isEmpty()) {
			throw new JiraSyncException("Found no transition to status '" + sourceStatusToSet + "'");
		} else if (filteredTransitions.size() > 1) {
			throw new JiraSyncException("Found multiple transitions to status " + sourceStatusToSet + ": " + filteredTransitions);
		} else {
			return filteredTransitions.get(0);
		}
	}

	private String getStatusName(JiraIssue issue) {
		JiraIssueStatus status = issue.getFields().getStatus();
		Assert.notNull(status, "status of " + issue + " must not be null");
		String statusName = status.getName();
		Assert.notNull(statusName, "statusName of " + issue + " must not be null");
		return statusName;
	}

	private void processDescription(JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		String existingDescription = descriptionMapper.getDescription(targetIssue);
		String newDescription = descriptionMapper.mapTargetDescription(sourceIssue, targetIssue);
		if (!Objects.equals(existingDescription, newDescription)) {
			issueUpdate.putFieldUpdate(JiraField.DESCRIPTION, newDescription);
		}
	}

	private void processLabels(JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate, JiraProjectSync projectSync) {
		Set<String> sourceValue = labelMapper.mapLabels(sourceIssue);
		Set<String> targetValue = labelMapper.mapLabels(targetIssue);

		Set<String> newValue = new LinkedHashSet<>(sourceValue);
		if (projectSync.getLabelsToKeepInTarget() != null) {
			for (String labelToKeep : projectSync.getLabelsToKeepInTarget()) {
				if (targetValue.contains(labelToKeep)) {
					newValue.add(labelToKeep);
				}
			}
		}

		if (!Objects.equals(newValue, targetValue)) {
			issueUpdate.putFieldUpdate(JiraField.LABELS, newValue);
		}
	}

	private void processPriority(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraPriority sourcePriority = priorityMapper.mapPriority(jiraTarget, sourceIssue);
		Assert.notNull(sourcePriority, "Priority of " + sourceIssue + " must not be null");
		JiraPriority targetPriority = targetIssue.getFields().getPriority();
		if (!isIdEqual(sourcePriority, targetPriority)) {
			issueUpdate.putFieldUpdate(JiraField.PRIORITY, sourcePriority);
		}
	}

	private void processResolution(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraResolution mappedTargetResolution = resolutionMapper.mapResolution(jiraSource, targetIssue);
		JiraResolution sourceResolution = sourceIssue.getFields().getResolution();
		if (!isIdEqual(mappedTargetResolution, sourceResolution)) {
			issueUpdate.putFieldUpdate(JiraField.RESOLUTION, mappedTargetResolution);
		}
	}

	private static <T extends JiraIdResource> boolean isIdEqual(T one, T other) {
		return Objects.equals(getId(one), getId(other));
	}

	private static String getId(JiraIdResource idResource) {
		if (idResource == null) {
			return null;
		} else {
			return idResource.getId();
		}
	}

	private void processVersions(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate, JiraProjectSync projectSync, Function<JiraIssueFields, Set<JiraVersion>> versionGetter, JiraField versionsField) {
		Set<JiraVersion> sourceVersions = versionGetter.apply(sourceIssue.getFields());
		Set<JiraVersion> targetVersions = versionGetter.apply(targetIssue.getFields());

		if (CollectionUtils.isEmpty(sourceVersions) && CollectionUtils.isEmpty(targetVersions)) {
			return;
		}

		Set<JiraVersion> mappedSourceVersions = DefaultVersionMapper.mapVersions(jiraTarget, sourceVersions, projectSync);

		if (!Objects.equals(targetVersions, mappedSourceVersions)) {
			issueUpdate.putFieldUpdate(versionsField, mappedSourceVersions);
		}
	}

	private void assertRequiredFieldsArePresent(JiraIssue issue) {
		Assert.notNull(issue.getKey(), "key must be set for " + issue);
		Assert.notNull(issue.getFields().getSummary(), "summary must be set for " + issue);
		Assert.notNull(issue.getFields().getPriority(), "priority must be set for " + issue);
		Assert.notNull(issue.getFields().getStatus(), "status must be set for " + issue);
	}
}
