package de.cronn.jira.sync.strategy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.config.SourceTargetStatus;
import de.cronn.jira.sync.domain.JiraIdResource;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraTransitions;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.ResolutionMapper;
import de.cronn.jira.sync.mapping.VersionMapper;
import de.cronn.jira.sync.resolve.JiraIssueResolver;
import de.cronn.jira.sync.service.JiraService;

@Component
public class UpdateExistingTargetJiraIssueSyncStrategy implements ExistingTargetJiraIssueSyncStrategy {

	private static final Logger log = LoggerFactory.getLogger(UpdateExistingTargetJiraIssueSyncStrategy.class);

	private final JiraIssueResolver jiraIssueResolver;
	private final JiraSyncConfig jiraSyncConfig;

	public UpdateExistingTargetJiraIssueSyncStrategy(JiraIssueResolver jiraIssueResolver, JiraSyncConfig jiraSyncConfig) {
		this.jiraIssueResolver = jiraIssueResolver;
		this.jiraSyncConfig = jiraSyncConfig;
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
		processResolution(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate);
		processVersions(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate, projectSync, JiraIssueFields::getVersions, "versions");
		processVersions(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate, projectSync, JiraIssueFields::getFixVersions, "fixVersions");

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
		Map<SourceTargetStatus, String> statusTransitions = projectSync.getStatusTransitions();
		if (statusTransitions != null) {
			String sourceIssueStatus = getStatusName(sourceIssue);
			String targetIssueStatus = getStatusName(targetIssue);
			String statusToTransitionTo = statusTransitions.get(new SourceTargetStatus(sourceIssueStatus, targetIssueStatus));
			if (statusToTransitionTo != null) {
				JiraTransition jiraTransition = getJiraTransition(jiraSource, sourceIssue, statusToTransitionTo);
				sourceIssueUpdate.setTransition(jiraTransition);
			}
		}
	}

	private JiraTransition getJiraTransition(JiraService jiraTarget, JiraIssue targetIssue, String statusToTransitionTo) {
		JiraTransitions allTransitions = jiraTarget.getTransitions(targetIssue);
		List<JiraTransition> filteredTransitions = allTransitions.getTransitions().stream()
			.filter(jiraTransition -> jiraTransition.getTo().getName().equals(statusToTransitionTo))
			.collect(Collectors.toList());

		if (filteredTransitions.isEmpty()) {
			throw new JiraSyncException("Found no transition to status '" + statusToTransitionTo + "'");
		} else if (filteredTransitions.size() > 1) {
			throw new JiraSyncException("Found multiple transitions to status " + statusToTransitionTo + ": " + filteredTransitions);
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
		String existingDescription = DescriptionMapper.getDescription(targetIssue);
		String newDescription = DescriptionMapper.mapTargetDescription(sourceIssue, targetIssue);
		if (!Objects.equals(existingDescription, newDescription)) {
			issueUpdate.putFieldUpdate("description", newDescription);
		}
	}

	private void processLabels(JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate, JiraProjectSync projectSync) {
		Set<String> sourceValue = LabelMapper.mapLabels(sourceIssue);
		Set<String> targetValue = LabelMapper.mapLabels(targetIssue);

		Set<String> newValue = new LinkedHashSet<>(sourceValue);
		if (projectSync.getLabelsToKeepInTarget() != null) {
			for (String labelToKeep : projectSync.getLabelsToKeepInTarget()) {
				if (targetValue.contains(labelToKeep)) {
					newValue.add(labelToKeep);
				}
			}
		}

		if (!Objects.equals(newValue, targetValue)) {
			issueUpdate.putFieldUpdate("labels", newValue);
		}
	}

	private void processPriority(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraPriority sourcePriority = PriorityMapper.mapPriority(jiraTarget, sourceIssue, jiraSyncConfig);
		Assert.notNull(sourcePriority, "Priority of " + sourceIssue + " must not be null");
		JiraPriority targetPriority = targetIssue.getFields().getPriority();
		if (!isIdEqual(sourcePriority, targetPriority)) {
			issueUpdate.putFieldUpdate("priority", sourcePriority);
		}
	}

	private void processResolution(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraResolution mappedTargetResolution = ResolutionMapper.mapResolution(jiraTarget, targetIssue, jiraSyncConfig);
		JiraResolution sourceResolution = sourceIssue.getFields().getResolution();
		if (!isIdEqual(mappedTargetResolution, sourceResolution)) {
			issueUpdate.putFieldUpdate("resolution", mappedTargetResolution);
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

	private void processVersions(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate, JiraProjectSync projectSync, Function<JiraIssueFields, Set<JiraVersion>> versionGetter, String versionsField) {
		Set<JiraVersion> sourceVersions = versionGetter.apply(sourceIssue.getFields());
		Set<JiraVersion> targetVersions = versionGetter.apply(targetIssue.getFields());

		if (CollectionUtils.isEmpty(sourceVersions) && CollectionUtils.isEmpty(targetVersions)) {
			return;
		}

		Set<JiraVersion> mappedSourceVersions = VersionMapper.mapVersions(jiraTarget, sourceVersions, projectSync);

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
