package de.cronn.jira.sync.strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.TransitionConfig;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraComments;
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
import de.cronn.jira.sync.mapping.CommentMapper;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.ResolutionMapper;
import de.cronn.jira.sync.mapping.VersionMapper;
import de.cronn.jira.sync.service.JiraService;

@Component
public class UpdateExistingTargetJiraIssueSyncStrategy implements ExistingTargetJiraIssueSyncStrategy {

	private static final Logger log = LoggerFactory.getLogger(UpdateExistingTargetJiraIssueSyncStrategy.class);

	private DescriptionMapper descriptionMapper;
	private LabelMapper labelMapper;
	private PriorityMapper priorityMapper;
	private ResolutionMapper resolutionMapper;
	private VersionMapper versionMapper;
	private CommentMapper commentMapper;

	@Autowired
	public void setDescriptionMapper(DescriptionMapper descriptionMapper) {
		this.descriptionMapper = descriptionMapper;
	}

	@Autowired
	public void setLabelMapper(LabelMapper labelMapper) {
		this.labelMapper = labelMapper;
	}

	@Autowired
	public void setPriorityMapper(PriorityMapper priorityMapper) {
		this.priorityMapper = priorityMapper;
	}

	@Autowired
	public void setVersionMapper(VersionMapper versionMapper) {
		this.versionMapper = versionMapper;
	}

	@Autowired
	public void setResolutionMapper(ResolutionMapper resolutionMapper) {
		this.resolutionMapper = resolutionMapper;
	}

	@Autowired
	public void setCommentMapper(CommentMapper commentMapper) {
		this.commentMapper = commentMapper;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		log.info("synchronizing '{}' with '{}'", sourceIssue.getKey(), targetIssue.getKey());

		assertRequiredFieldsArePresent(sourceIssue);
		assertRequiredFieldsArePresent(targetIssue);

		JiraIssueUpdate targetIssueUpdate = new JiraIssueUpdate();
		JiraIssueUpdate sourceIssueUpdate = new JiraIssueUpdate();

		processTransition(jiraSource, sourceIssue, targetIssue, projectSync, sourceIssueUpdate);
		processDescription(sourceIssue, targetIssue, targetIssueUpdate);
		processLabels(sourceIssue, targetIssue, targetIssueUpdate, projectSync);
		processPriority(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate);
		processVersions(jiraTarget, sourceIssue, targetIssue, JiraIssueFields::getVersions, versions -> targetIssueUpdate.getOrCreateFields().setVersions(versions), projectSync);
		processVersions(jiraTarget, sourceIssue, targetIssue, JiraIssueFields::getFixVersions, versions -> targetIssueUpdate.getOrCreateFields().setFixVersions(versions), projectSync);

		if (projectSync.isCopyCommentsToTarget()) {
			processComments(sourceIssue, targetIssue, jiraSource, jiraTarget);
		}

		if (sourceIssueUpdate.isEmpty() && targetIssueUpdate.isEmpty()) {
			return SyncResult.UNCHANGED;
		}

		if (!sourceIssueUpdate.isEmpty()) {
			if (sourceIssueUpdate.getTransition() != null) {
				jiraSource.transitionIssue(sourceIssue.getKey(), sourceIssueUpdate);
			} else {
				log.warn("Ignoring source issue update of {} without transition", sourceIssue);
			}
		}

		if (!targetIssueUpdate.isEmpty()) {
			Assert.isNull(targetIssueUpdate.getTransition());
			jiraTarget.updateIssue(targetIssue.getKey(), targetIssueUpdate);
		}

		if (sourceIssueUpdate.getTransition() != null) {
			return SyncResult.CHANGED_TRANSITION;
		} else {
			return SyncResult.CHANGED;
		}
	}

	private void processComments(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget) {
		List<JiraComment> commentsInSource = getComments(sourceIssue);
		List<JiraComment> commentsInTarget = getComments(targetIssue);

		List<JiraComment> newCommentsInSource = commentsInSource.stream()
			.filter(commentInSource -> !isCommentInTargetIssue(commentInSource, commentsInTarget))
			.collect(Collectors.toList());

		List<JiraComment> commentsOnlyInTarget = commentsInTarget.stream()
			.filter(commentInTarget -> !isCommentInSourceIssue(commentInTarget, commentsInSource))
			.collect(Collectors.toList());

		boolean behindTime = isCommentBehindTime(commentsInTarget, newCommentsInSource, commentsOnlyInTarget);

		for (JiraComment commentInSource : newCommentsInSource) {
			String commentText = commentMapper.map(sourceIssue, commentInSource, jiraSource, behindTime);
			log.info("adding comment {}", commentInSource.getId());
			jiraTarget.addComment(targetIssue.getKey(), commentText);
		}
	}

	private boolean isCommentBehindTime(List<JiraComment> commentsInTarget, List<JiraComment> newCommentsInSource, List<JiraComment> commentsOnlyInTarget) {
		if (!commentsOnlyInTarget.isEmpty() && !newCommentsInSource.isEmpty()) {
			JiraComment latestCommentInTarget = commentsInTarget.get(commentsInTarget.size() - 1);
			JiraComment firstCommentInSource = newCommentsInSource.get(0);
			return (latestCommentInTarget.getUpdated().isAfter(firstCommentInSource.getCreated()));
		} else {
			return false;
		}
	}

	private List<JiraComment> getComments(JiraIssue issue) {
		JiraComments comment = issue.getFields().getComment();
		if (comment == null) {
			return Collections.emptyList();
		}
		List<JiraComment> comments = comment.getComments();
		if (comments == null) {
			return Collections.emptyList();
		}
		return comments.stream()
			.sorted(Comparator.comparing(JiraComment::getCreated))
			.collect(Collectors.toList());
	}

	private boolean isCommentInTargetIssue(JiraComment commentInSource, List<JiraComment> commentsInTarget) {
		for (JiraComment commentInTarget : commentsInTarget) {
			if (commentMapper.isMapped(commentInSource, commentInTarget.getBody())) {
				return true;
			}
		}
		return false;
	}

	private boolean isCommentInSourceIssue(JiraComment commentInTarget, List<JiraComment> commentsInSource) {
		for (JiraComment commentInSource : commentsInSource) {
			if (commentMapper.isMapped(commentInSource, commentInTarget.getBody())) {
				return true;
			}
		}
		return false;
	}

	private void processTransition(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync, JiraIssueUpdate sourceIssueUpdate) {
		Map<String, TransitionConfig> transitions = projectSync.getTransitions();
		if (transitions.isEmpty()) {
			log.trace("No transitions configured");
			return;
		}

		TransitionConfig transition = findTransition(sourceIssue, targetIssue, transitions.values());
		if (transition != null) {
			log.info("triggering transition from status '{}' to '{}'", sourceIssue.getFields().getStatus().getName(), transition.getSourceStatusToSet());
			if (transition.isAssignToMyselfInSource()) {
				JiraUser myself = jiraSource.getMyself();
				if (!isEqual(sourceIssue, myself)) {
					JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate();
					jiraIssueUpdate.getOrCreateFields().setAssignee(myself);
					jiraSource.updateIssue(sourceIssue.getKey(), jiraIssueUpdate);
				}
			}

			JiraTransition issueTransition = findIssueTransition(jiraSource, sourceIssue, transition);
			sourceIssueUpdate.setTransition(issueTransition);

			if (transition.isCopyResolutionToSource()) {
				processResolution(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate);
			}

			if (transition.isCopyFixVersionsToSource()) {
				processVersions(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate, projectSync);
			}
		}
	}

	private void processVersions(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate sourceIssueUpdate, JiraProjectSync projectSync) {
		Set<JiraVersion> targetFixVersions = targetIssue.getFields().getFixVersions();
		Set<JiraVersion> sourceFixVersions = sourceIssue.getFields().getFixVersions();

		if (CollectionUtils.isEmpty(sourceFixVersions) && CollectionUtils.isEmpty(targetFixVersions)) {
			return;
		}

		Set<JiraVersion> mappedVersions = versionMapper.mapTargetToSource(jiraSource, targetFixVersions, projectSync);
		if (!Objects.equals(mappedVersions, sourceFixVersions)) {
			sourceIssueUpdate.getOrCreateFields().setFixVersions(mappedVersions);
			sourceIssue.getFields().setFixVersions(mappedVersions);
		}
	}

	private boolean isEqual(JiraIssue issue, JiraUser jiraUser) {
		JiraUser assignee = issue.getFields().getAssignee();
		if (assignee == null) {
			return false;
		}
		return Objects.equals(jiraUser.getKey(), assignee.getKey());
	}

	private TransitionConfig findTransition(JiraIssue sourceIssue, JiraIssue targetIssue, Collection<TransitionConfig> transitions) {
		String sourceIssueStatus = getStatusName(sourceIssue);
		String targetIssueStatus = getStatusName(targetIssue);

		List<TransitionConfig> transitionConfigs = transitions.stream()
			.filter(transitionConfig -> transitionConfig.getSourceStatusIn().contains(sourceIssueStatus))
			.filter(transitionConfig -> transitionConfig.getTargetStatusIn().contains(targetIssueStatus))
			.filter(transitionConfig -> filterOnlyIfAssignedInTarget(transitionConfig, targetIssue))
			.collect(Collectors.toList());

		if (transitionConfigs.isEmpty()) {
			return null;
		} else if (transitionConfigs.size() == 1) {
			TransitionConfig transitionConfig = transitionConfigs.get(0);
			String statusToTransitionTo = transitionConfig.getSourceStatusToSet();
			Assert.notNull(statusToTransitionTo);
			return transitionConfig;
		} else {
			throw new JiraSyncException("Illegal number of matching transitions: " + transitionConfigs);
		}
	}

	private boolean filterOnlyIfAssignedInTarget(TransitionConfig transitionConfig, JiraIssue targetIssue) {
		if (transitionConfig.isOnlyIfAssignedInTarget()) {
			return targetIssue.getFields().getAssignee() != null;
		} else {
			return true;
		}
	}

	private JiraTransition findIssueTransition(JiraService jiraTarget, JiraIssue targetIssue, TransitionConfig transitionConfig) {
		String sourceStatusToSet = transitionConfig.getSourceStatusToSet();
		List<JiraTransition> allTransitions = jiraTarget.getTransitions(targetIssue.getKey());
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
			issueUpdate.getOrCreateFields().setDescription(newDescription);
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
			issueUpdate.getOrCreateFields().setLabels(newValue);
		}
	}

	private void processPriority(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraPriority sourcePriority = priorityMapper.mapPriority(jiraTarget, sourceIssue);
		Assert.notNull(sourcePriority, "Priority of " + sourceIssue + " must not be null");
		JiraPriority targetPriority = targetIssue.getFields().getPriority();
		if (!isIdEqual(sourcePriority, targetPriority)) {
			issueUpdate.getOrCreateFields().setPriority(sourcePriority);
		}
	}

	private void processResolution(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate) {
		JiraResolution mappedResolution = resolutionMapper.mapResolution(jiraSource, targetIssue);
		JiraResolution sourceResolution = sourceIssue.getFields().getResolution();
		if (!isIdEqual(mappedResolution, sourceResolution)) {
			issueUpdate.getOrCreateFields().setResolution(mappedResolution);
			sourceIssue.getFields().setResolution(mappedResolution);
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

	private void processVersions(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, Function<JiraIssueFields, Set<JiraVersion>> versionGetter, Consumer<Set<JiraVersion>> versionsSetter, JiraProjectSync projectSync) {
		Set<JiraVersion> sourceVersions = versionGetter.apply(sourceIssue.getFields());
		Set<JiraVersion> targetVersions = versionGetter.apply(targetIssue.getFields());

		if (CollectionUtils.isEmpty(sourceVersions) && CollectionUtils.isEmpty(targetVersions)) {
			return;
		}

		Set<JiraVersion> mappedSourceVersions = versionMapper.mapSourceToTarget(jiraTarget, sourceVersions, projectSync);

		if (!Objects.equals(targetVersions, mappedSourceVersions)) {
			versionsSetter.accept(mappedSourceVersions);
		}
	}

	private void assertRequiredFieldsArePresent(JiraIssue issue) {
		Assert.notNull(issue.getKey(), "key must be set for " + issue);
		Assert.notNull(issue.getFields().getSummary(), "summary must be set for " + issue);
		Assert.notNull(issue.getFields().getPriority(), "priority must be set for " + issue);
		Assert.notNull(issue.getFields().getStatus(), "status must be set for " + issue);
	}
}
