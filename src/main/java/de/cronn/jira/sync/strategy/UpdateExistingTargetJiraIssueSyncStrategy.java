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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.SetUtils;
import de.cronn.jira.sync.config.Context;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.TransitionConfig;
import de.cronn.jira.sync.domain.JiraChangeLog;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraComments;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIdResource;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraNamedResource;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.domain.WellKnownCustomFieldType;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.mapping.CommentMapper;
import de.cronn.jira.sync.mapping.ComponentMapper;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.FieldMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.NamedResourceMapper;
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
	private ComponentMapper componentMapper;
	private CommentMapper commentMapper;
	private JiraIssueLinker issueLinker;
	private FieldMapper fieldMapper;

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
	public void setComponentMapper(ComponentMapper componentMapper) {
		this.componentMapper = componentMapper;
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

	@Autowired
	public void setFieldMapper(FieldMapper fieldMapper) {
		this.fieldMapper = fieldMapper;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		log.info("synchronizing '{}' with '{}'", sourceIssue.getKey(), targetIssue.getKey());

		assertRequiredFieldsArePresent(sourceIssue);
		assertRequiredFieldsArePresent(targetIssue);

		JiraIssueUpdate targetIssueUpdate = new JiraIssueUpdate();
		JiraIssueUpdate sourceIssueUpdate = new JiraIssueUpdate();

		processTransition(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync, sourceIssueUpdate, targetIssueUpdate);
		processDescription(sourceIssue, targetIssue, targetIssueUpdate, jiraSource);
		processLabels(sourceIssue, targetIssue, targetIssueUpdate, projectSync);
		processPriority(jiraTarget, sourceIssue, targetIssue, targetIssueUpdate);
		processNamedResources(jiraTarget, sourceIssue, targetIssue, JiraIssueFields::getVersions, versions -> targetIssueUpdate.getOrCreateFields().setVersions(versions), projectSync, versionMapper);
		processNamedResources(jiraTarget, sourceIssue, targetIssue, JiraIssueFields::getFixVersions, versions -> targetIssueUpdate.getOrCreateFields().setFixVersions(versions), projectSync, versionMapper);
		processNamedResources(jiraTarget, sourceIssue, targetIssue, JiraIssueFields::getComponents, components -> targetIssueUpdate.getOrCreateFields().setComponents(components), projectSync, componentMapper);
		processCustomFields(jiraSource, jiraTarget, sourceIssue, targetIssue, targetIssueUpdate);

		if (projectSync.isCopyCommentsToTarget() && !shouldSkipUpdate(targetIssue, projectSync)) {
			processComments(sourceIssue, targetIssue, jiraSource, jiraTarget);
		}

		if (sourceIssueUpdate.isEmpty() && targetIssueUpdate.isEmpty()) {
			return SyncResult.UNCHANGED;
		}

		processSourceIssueUpdate(jiraSource, sourceIssue, sourceIssueUpdate);
		processTargetIssueUpdate(jiraTarget, targetIssue, targetIssueUpdate, projectSync);

		if (sourceIssueUpdate.getTransition() != null || targetIssueUpdate.getTransition() != null) {
			return SyncResult.CHANGED_TRANSITION;
		} else {
			return SyncResult.CHANGED;
		}
	}

	private void processTargetIssueUpdate(JiraService jiraTarget, JiraIssue targetIssue, JiraIssueUpdate targetIssueUpdate, JiraProjectSync projectSync) {
		if (!targetIssueUpdate.isEmpty()) {
			if (targetIssueUpdate.getTransition() != null) {
				jiraTarget.transitionIssue(targetIssue.getKey(), targetIssueUpdate);
			} else {
				if (shouldSkipUpdate(targetIssue, projectSync)) {
					log.debug("skipping update of {} in status {}", targetIssue, targetIssue.getFields().getStatus().getName());
				} else {
					log.info("updating issue");
					jiraTarget.updateIssue(targetIssue.getKey(), targetIssueUpdate);
				}
			}
		}
	}

	private void processSourceIssueUpdate(JiraService jiraSource, JiraIssue sourceIssue, JiraIssueUpdate sourceIssueUpdate) {
		if (!sourceIssueUpdate.isEmpty()) {
			if (sourceIssueUpdate.getTransition() != null) {
				jiraSource.transitionIssue(sourceIssue.getKey(), sourceIssueUpdate);
			} else {
				log.warn("Ignoring source issue update of {} without transition", sourceIssue);
			}
		}
	}

	private boolean shouldSkipUpdate(JiraIssue targetIssue, JiraProjectSync projectSync) {
		return projectSync.getSkipUpdateInTargetWhenStatusIn().contains(targetIssue.getFields().getStatus().getName());
	}

	private void processCustomFields(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraIssueUpdate targetIssueUpdate) {
		JiraProject targetProject = targetIssue.getFields().getProject();
		Map<String, Object> mappedFields = fieldMapper.map(sourceIssue, jiraSource, jiraTarget, targetProject);
		for (Entry<String, Object> entry : mappedFields.entrySet()) {
			String fieldId = entry.getKey();
			Object existingValue = targetIssue.getOrCreateFields().getOther().get(fieldId);
			JiraField fieldInTarget = jiraTarget.findFieldById(fieldId);
			if (!haveSameValue(fieldInTarget, existingValue, entry.getValue())) {
				targetIssueUpdate.getOrCreateFields().setOther(fieldId, entry.getValue());
			}
		}
	}

	private boolean haveSameValue(JiraField fieldInTarget, Object existingValue, Object newValue) {
		WellKnownCustomFieldType fieldType = WellKnownCustomFieldType.getByCustomSchema(fieldInTarget.getSchema());
		if (fieldType.equals(WellKnownCustomFieldType.SELECT)) {
			return equalsCustomFieldSelect(existingValue, newValue);
		} else {
			return Objects.equals(existingValue, newValue);
		}
	}

	private static boolean equalsCustomFieldSelect(Object existingValue, Object newValue) {
		if (existingValue == newValue) {
			return true;
		}
		if (existingValue == null || newValue == null) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Object oneValue = ((Map<String, Object>) existingValue).get("value");
		@SuppressWarnings("unchecked")
		Object otherValue = ((Map<String, Object>) newValue).get("value");
		return Objects.equals(oneValue, otherValue);
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
		if (isNotEqual(commentText, targetComment.getBody())) {
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

	private JiraComment findMatchingCommentInSourceIssue(JiraComment commentInTarget, List<JiraComment> commentsInSource) {
		for (JiraComment commentInSource : commentsInSource) {
			if (commentMapper.isMapped(commentInSource, commentInTarget.getBody())) {
				return commentInSource;
			}
		}
		return null;
	}

	private void processTransition(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync, JiraIssueUpdate sourceIssueUpdate, JiraIssueUpdate targetIssueUpdate) {
		Map<String, TransitionConfig> transitions = projectSync.getTransitions();
		if (transitions.isEmpty()) {
			log.trace("No transitions configured");
			return;
		}

		TransitionConfig transition = findTransition(sourceIssue, targetIssue, transitions.values(), jiraTarget, jiraSource);
		if (transition != null) {
			if (transition.isAssignToMyselfInSource()) {
				JiraUser myself = jiraSource.getMyself();
				if (!isEqual(sourceIssue, myself)) {
					jiraSource.updateIssue(sourceIssue.getKey(), fields -> fields.setAssignee(myself));
				}
			}

			String sourceStatusToSet = transition.getSourceStatusToSet();
			if (sourceStatusToSet != null) {
				JiraTransition issueTransition = findIssueTransition(jiraSource, sourceIssue, sourceStatusToSet);
				sourceIssueUpdate.setTransition(issueTransition);

				if (transition.isCopyResolutionToSource()) {
					processResolution(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate);
				}

				if (transition.isCopyFixVersionsToSource()) {
					processFixVersions(jiraSource, sourceIssue, targetIssue, sourceIssueUpdate, projectSync);
				}

				copyCustomFields(jiraSource, sourceIssue, targetIssue, jiraTarget, sourceIssueUpdate, transition);
			} else {
				String targetStatusToSet = transition.getTargetStatusToSet();
				Assert.notNull(targetStatusToSet, "Expected targetStatusToSet in " + transition);

				JiraTransition issueTransition = findIssueTransition(jiraTarget, targetIssue, targetStatusToSet);
				targetIssueUpdate.setTransition(issueTransition);

				Assert.isTrue(!transition.isCopyFixVersionsToSource(), "Unexpected property set for " + transition + ": copyFixVersionsToSource");
				Assert.isTrue(!transition.isCopyResolutionToSource(), "Unexpected property set for " + transition + ": copyResolutionToSource");
				Assert.isTrue(transition.getCustomFieldsToCopyFromTargetToSource(sourceIssue).isEmpty(), "Unexpected properties set for " + transition + ": customFieldsToCopyFromTargetToSource");
			}
		}
	}

	private void copyCustomFields(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraTarget, JiraIssueUpdate sourceIssueUpdate, TransitionConfig transition) {
		Map<String, String> customFieldsToCopyFromSourceToTarget = transition.getCustomFieldsToCopyFromTargetToSource(sourceIssue);
		for (Entry<String, String> entry : customFieldsToCopyFromSourceToTarget.entrySet()) {
			String customFieldNameInSource = entry.getKey();
			String customFieldNameInTarget = entry.getValue();
			JiraField customFieldInTarget = jiraTarget.findField(customFieldNameInTarget);
			JiraField customFieldInSource = jiraSource.findField(customFieldNameInSource);
			Assert.isTrue(customFieldInTarget.isCustom(), customFieldInTarget + " is not a custom field in target");
			Assert.isTrue(customFieldInSource.isCustom(), customFieldNameInSource + " is not a custom field in source");

			JiraProject sourceProject = sourceIssue.getFields().getProject();
			Object mappedValue = fieldMapper.mapValue(targetIssue, customFieldInTarget, customFieldInSource, jiraSource, sourceProject);
			sourceIssueUpdate.getOrCreateFields().setOther(customFieldInSource.getId(), mappedValue);
			sourceIssue.getFields().setOther(customFieldInSource.getId(), mappedValue);
		}
	}

	private void processFixVersions(JiraService jiraSource, JiraIssue sourceIssue, JiraIssue targetIssue,
									JiraIssueUpdate sourceIssueUpdate, JiraProjectSync projectSync) {
		Set<JiraVersion> targetFixVersions = targetIssue.getFields().getFixVersions();
		Set<JiraVersion> sourceFixVersions = sourceIssue.getFields().getFixVersions();

		if (CollectionUtils.isEmpty(sourceFixVersions) && CollectionUtils.isEmpty(targetFixVersions)) {
			return;
		}

		Set<JiraVersion> mappedVersions = versionMapper.mapTargetToSource(jiraSource, targetFixVersions, projectSync);

		for (JiraVersion unmappedVersion : collectUnmappedVersions(projectSync, sourceFixVersions)) {
			log.warn("Keeping unmapped version in source: {}", unmappedVersion);
			mappedVersions.add(unmappedVersion);
		}

		if (!Objects.equals(mappedVersions, sourceFixVersions)) {
			sourceIssueUpdate.getOrCreateFields().setFixVersions(mappedVersions);
			sourceIssue.getFields().setFixVersions(mappedVersions);
		}
	}

	private static Set<JiraVersion> collectUnmappedVersions(JiraProjectSync projectSync, Set<JiraVersion> sourceVersions) {
		if (CollectionUtils.isEmpty(sourceVersions)) {
			return Collections.emptySet();
		}
		return sourceVersions.stream()
			.filter(version -> !projectSync.getVersionMapping().containsKey(version.getName()))
			.collect(SetUtils.toLinkedHashSet());
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
			.filter(transitionConfig -> filterOnlyIfStatusTransitionNewerInSource(transitionConfig, sourceIssue, targetIssue, jiraSource, jiraTarget))
			.filter(transitionConfig -> filterOnlyIfStatusTransitionNewerInTarget(transitionConfig, sourceIssue, targetIssue, jiraSource, jiraTarget))
			.collect(Collectors.toList());

		if (transitionConfigs.isEmpty()) {
			return null;
		} else if (transitionConfigs.size() == 1) {
			TransitionConfig transitionConfig = transitionConfigs.get(0);
			String sourceStatusToSet = transitionConfig.getSourceStatusToSet();
			String targetStatusToSet = transitionConfig.getTargetStatusToSet();
			if ((sourceStatusToSet != null && targetStatusToSet != null)
				|| (sourceStatusToSet == null && targetStatusToSet == null)) {
				throw new IllegalArgumentException("Need to set either 'sourceStatusToSet' or 'targetStatusToSet' in " + transitionConfig);
			}
			return transitionConfig;
		} else {
			throw new JiraSyncException("Illegal number of matching transitions: " + transitionConfigs);
		}
	}

	private boolean filterOnlyIfStatusTransitionNewerInTarget(TransitionConfig transitionConfig, JiraIssue sourceIssue,
															  JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget) {

		if (transitionConfig.getOnlyIfStatusTransitionNewerIn() != Context.TARGET) {
			return true;
		}

		return isLeftStatusTransitionNewer(targetIssue, sourceIssue, jiraTarget, jiraSource, transitionConfig);
	}

	private boolean filterOnlyIfStatusTransitionNewerInSource(TransitionConfig transitionConfig, JiraIssue sourceIssue,
															  JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget) {

		if (transitionConfig.getOnlyIfStatusTransitionNewerIn() != Context.SOURCE) {
			return true;
		}

		return isLeftStatusTransitionNewer(sourceIssue, targetIssue, jiraSource, jiraTarget, transitionConfig);
	}

	private boolean isLeftStatusTransitionNewer(JiraIssue leftIssue, JiraIssue rightIssue, JiraService leftJiraService,
		JiraService rightJiraService, TransitionConfig transition) {
		ZonedDateTime latestStatusTransitionLeft = getLatestStatusTransitionDate(leftIssue, leftJiraService);

		ZonedDateTime latestStatusTransitionRight = getLatestStatusTransitionDate(rightIssue, rightJiraService);

		if (latestStatusTransitionLeft == null) {
			log.debug("Status transition of {} at {} is not newer than {} at {}. Skipping {}.", leftIssue,
				latestStatusTransitionLeft, rightIssue, latestStatusTransitionRight, transition);
			return false;
		}

		if (latestStatusTransitionRight == null) {
			log.debug("Status transition of {} at {} is newer than {} at {}. Not skipping {}.", leftIssue,
				latestStatusTransitionLeft, rightIssue, latestStatusTransitionRight, transition);
			return true;
		}

		if (latestStatusTransitionLeft.isAfter(latestStatusTransitionRight)) {
			log.debug("Status transition of {} at {} is newer than {} at {}. Not skipping {}.", leftIssue,
				latestStatusTransitionLeft, rightIssue, latestStatusTransitionRight, transition);
			return true;
		} else {
			log.debug("Status transition of {} at {} is not newer than {} at {}. Skipping {}.", leftIssue,
				latestStatusTransitionLeft, rightIssue, latestStatusTransitionRight, transition);
			return false;
		}
	}

	private ZonedDateTime getLatestStatusTransitionDate(JiraIssue issue, JiraService jiraService) {
		JiraIssue issueWithChangelog = jiraService.getIssueByKeyWithChangelog(issue.getKey());
		ZonedDateTime latestStatusTransitionDate = Optional.ofNullable(issueWithChangelog.getChangelog())
			.map(JiraChangeLog::getLatestStatusTransition)
			.map(JiraIssueHistoryEntry::getCreated)
			.orElse(null);

		return latestStatusTransitionDate;
	}

	private boolean filterOnlyIfAssignedInTarget(TransitionConfig transitionConfig, JiraIssue targetIssue) {
		if (transitionConfig.isOnlyIfAssignedInTarget()) {
			return targetIssue.getFields().getAssignee() != null;
		} else {
			return true;
		}
	}

	private boolean filterIssueWasMovedBetweenProjects(TransitionConfig transitionConfig, JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraTarget, JiraService jiraSource) {
		if (transitionConfig.isTriggerIfIssueWasMovedBetweenProjects()) {
			return true;
		}
		String key = issueLinker.resolveKey(targetIssue, jiraTarget, jiraSource);
		boolean sameKey = Objects.equals(key, sourceIssue.getKey());
		if (!sameKey) {
			log.debug("Skipping updates of {} since the key of the source issue changed: {} vs. {}", targetIssue, key, sourceIssue.getKey());
		}
		return sameKey;
	}

	private JiraTransition findIssueTransition(JiraService jiraService, JiraIssue issue, String statusToTransitionTo) {
		List<JiraTransition> allTransitions = jiraService.getTransitions(issue.getKey());
		List<JiraTransition> filteredTransitions = allTransitions.stream()
			.filter(jiraTransition -> jiraTransition.getTo().getName().equals(statusToTransitionTo))
			.collect(Collectors.toList());

		if (filteredTransitions.isEmpty()) {
			throw new JiraSyncException("Found no transition to status '" + statusToTransitionTo + "'");
		} else if (filteredTransitions.size() > 1) {
			throw new JiraSyncException("Found multiple transitions to status '" + statusToTransitionTo + "': " + filteredTransitions);
		} else {
			JiraTransition transition = filteredTransitions.get(0);
			log.info("triggering transition from status '{}' to '{}': {}", issue.getFields().getStatus().getName(), statusToTransitionTo, transition);
			Assert.isTrue(Objects.equals(transition.getTo().getName(), statusToTransitionTo), "Unexpected issue transition: " + transition);
			return transition;
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
		if (isNotEqual(existingDescription, newDescription)) {
			issueUpdate.getOrCreateFields().setDescription(newDescription);
		}
	}

	private static boolean isNotEqual(String one, String other) {
		return !isEqual(one, other);
	}

	private static boolean isEqual(String one, String other) {
		if (StringUtils.isEmpty(one) && StringUtils.isEmpty(other)) {
			return true;
		}
		return Objects.equals(one, other);
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
		if (sourcePriority == null) {
			log.debug("Skipping unknown priority {} of {}", sourceIssue.getFields().getPriority(), sourceIssue);
			return;
		}
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

	private static <T extends JiraNamedResource> void processNamedResources(JiraService jiraTarget, JiraIssue sourceIssue,
																			JiraIssue targetIssue,
																			Function<JiraIssueFields, Set<T>> resourceGetter,
																			Consumer<Set<T>> resourceSetter,
																			JiraProjectSync projectSync,
																			NamedResourceMapper<T> resourceMapper) {
		Set<T> sourceResources = resourceGetter.apply(sourceIssue.getFields());
		Set<T> targetResources = resourceGetter.apply(targetIssue.getFields());

		Set<T> mappedResources = resourceMapper.mapSourceToTarget(jiraTarget, sourceResources, projectSync);

		if (CollectionUtils.isEmpty(sourceResources) && CollectionUtils.isEmpty(mappedResources)) {
			return;
		}

		if (!Objects.equals(targetResources, mappedResources)) {
			resourceSetter.accept(mappedResources);
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
