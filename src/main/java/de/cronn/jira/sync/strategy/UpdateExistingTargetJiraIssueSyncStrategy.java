package de.cronn.jira.sync.strategy;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import de.cronn.jira.sync.link.JiraIssueLinker;
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
	private JiraIssueLinker issueLinker;

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

	@Autowired
	public void setIssueLinker(JiraIssueLinker issueLinker) {
		this.issueLinker = issueLinker;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		log.info("synchronizing '{}' with '{}'", sourceIssue.getKey(), targetIssue.getKey());

		assertRequiredFieldsArePresent(sourceIssue);
		assertRequiredFieldsArePresent(targetIssue);

		JiraIssueUpdate targetIssueUpdate = new JiraIssueUpdate();
		JiraIssueUpdate sourceIssueUpdate = new JiraIssueUpdate();

		processTransition(jiraSource, sourceIssue, targetIssue, projectSync, sourceIssueUpdate, jiraTarget);
		processDescription(sourceIssue, targetIssue, targetIssueUpdate, jiraSource);
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
			log.info("updating issue");
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

		Map<JiraComment, JiraComment> targetToSourceAssociations = associateComments(commentsInSource, commentsInTarget);

		List<JiraComment> newCommentsInSource = commentsInSource.stream()
			.filter(commentInSource -> !targetToSourceAssociations.containsValue(commentInSource))
			.collect(Collectors.toList());

		JiraComment latestCommentOnlyInTarget = findLatestCommentOnlyInTarget(targetToSourceAssociations);

		updateExistingComments(sourceIssue, targetIssue, jiraSource, jiraTarget, targetToSourceAssociations);

		boolean behindTime = isCommentBehindTime(newCommentsInSource, latestCommentOnlyInTarget);

		for (JiraComment commentInSource : newCommentsInSource) {
			String commentText = commentMapper.map(sourceIssue, commentInSource, jiraSource, behindTime);
			log.info("adding comment {}", commentInSource.getId());
			jiraTarget.addComment(targetIssue.getKey(), commentText);
		}
	}

	private void updateExistingComments(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget, Map<JiraComment, JiraComment> targetToSourceAssociations) {
		for (Entry<JiraComment, JiraComment> entry : targetToSourceAssociations.entrySet()) {
			JiraComment sourceComment = entry.getValue();
			if (sourceComment != null) {
				JiraComment targetComment = entry.getKey();
				updateComment(sourceIssue, targetIssue, sourceComment, targetComment, jiraSource, jiraTarget);
			}
		}
	}

	private void updateComment(JiraIssue sourceIssue, JiraIssue targetIssue, JiraComment sourceComment, JiraComment targetComment, JiraService jiraSource, JiraService jiraTarget) {
		boolean behindTime = commentMapper.wasAddedBehindTime(targetComment);
		String commentText = commentMapper.map(sourceIssue, sourceComment, jiraSource, behindTime);
		if (!Objects.equals(commentText, targetComment.getBody())) {
			log.info("updating comment {}", targetComment.getId());
			jiraTarget.updateComment(targetIssue.getKey(), targetComment.getId(), commentText);
		}
	}

	private JiraComment findLatestCommentOnlyInTarget(Map<JiraComment, JiraComment> targetToSourceAssociations) {
		JiraComment latestComment = null;
		for (Entry<JiraComment, JiraComment> entry : targetToSourceAssociations.entrySet()) {
			if (entry.getValue() == null) {
				latestComment = entry.getKey();
			}
		}
		return latestComment;
	}

	private Map<JiraComment, JiraComment> associateComments(List<JiraComment> commentsInSource, List<JiraComment> commentsInTarget) {
		Map<JiraComment, JiraComment> targetCommentToSourceComments = new LinkedHashMap<>();

		for (JiraComment commentInTarget : commentsInTarget) {
			JiraComment commentInSource = findMatchingCommentInSourceIssue(commentInTarget, commentsInSource);
			targetCommentToSourceComments.put(commentInTarget, commentInSource);
		}
		return targetCommentToSourceComments;
	}

	private boolean isCommentBehindTime(List<JiraComment> newCommentsInSource, JiraComment latestCommentOnlyInTarget) {
		if (latestCommentOnlyInTarget == null || newCommentsInSource.isEmpty()) {
			return false;
		}
		JiraComment firstCommentInSource = newCommentsInSource.get(0);
		ZonedDateTime updatedInTarget = latestCommentOnlyInTarget.getUpdated();
		ZonedDateTime createdInSource = firstCommentInSource.getCreated();
		return updatedInTarget.isAfter(createdInSource);
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

	private JiraComment findMatchingCommentInSourceIssue(JiraComment commentInTarget, List<JiraComment> commentsInSource) {
		for (JiraComment commentInSource : commentsInSource) {
			if (commentMapper.isMapped(commentInSource, commentInTarget.getBody())) {
				return commentInSource;
			}
		}
		return null;
	}

	private void processTransition(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync, JiraIssueUpdate sourceIssueUpdate, JiraService jiraTarget) {
		Map<String, TransitionConfig> transitions = projectSync.getTransitions();
		if (transitions.isEmpty()) {
			log.trace("No transitions configured");
			return;
		}

		TransitionConfig transition = findTransition(sourceIssue, targetIssue, transitions.values(), jiraTarget, jiraSource);
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

	private TransitionConfig findTransition(JiraIssue sourceIssue, JiraIssue targetIssue, Collection<TransitionConfig> transitions, JiraService jiraTarget, JiraService jiraSource) {
		String sourceIssueStatus = getStatusName(sourceIssue);
		String targetIssueStatus = getStatusName(targetIssue);

		List<TransitionConfig> transitionConfigs = transitions.stream()
			.filter(transitionConfig -> transitionConfig.getSourceStatusIn().contains(sourceIssueStatus))
			.filter(transitionConfig -> transitionConfig.getTargetStatusIn().contains(targetIssueStatus))
			.filter(transitionConfig -> filterOnlyIfAssignedInTarget(transitionConfig, targetIssue))
			.filter(transitionConfig -> filterIssueWasMovedBetweenProjects(transitionConfig, sourceIssue, targetIssue, jiraTarget, jiraSource))
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

	private boolean filterIssueWasMovedBetweenProjects(TransitionConfig transitionConfig, JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraTarget, JiraService jiraSource) {
		if (!transitionConfig.isTriggerIfIssueWasMovedBetweenProjects()) {
			String key = issueLinker.resolveKey(targetIssue, jiraTarget, jiraSource);
			return Objects.equals(key, sourceIssue.getKey());
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
			throw new JiraSyncException("Found multiple transitions to status '" + sourceStatusToSet + "': " + filteredTransitions);
		} else {
			return filteredTransitions.get(0);
		}
	}

	private String getStatusName(JiraIssue issue) {
		JiraIssueStatus status = issue.getFields().getStatus();
		assertFieldNotNull(issue, status, "status");
		String statusName = status.getName();
		assertFieldNotNull(issue, statusName, "statusName");
		return statusName;
	}

	private void processDescription(JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate issueUpdate, JiraService jiraSource) {
		String existingDescription = descriptionMapper.getDescription(targetIssue);
		String newDescription = descriptionMapper.mapTargetDescription(sourceIssue, targetIssue, jiraSource);
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
		assertFieldNotNull(sourceIssue, sourcePriority, "priority");
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
		assertFieldNotNull(issue, issue.getKey(), "key");
		assertFieldNotNull(issue, issue.getFields().getSummary(), "summary");
		assertFieldNotNull(issue, issue.getFields().getPriority(), "priority");
		assertFieldNotNull(issue, issue.getFields().getStatus(), "status");
	}

	private void assertFieldNotNull(JiraIssue issue, Object fieldValue, String fieldName) {
		Assert.notNull(fieldValue, fieldName + " of " + issue + " must not be null");
	}
}
