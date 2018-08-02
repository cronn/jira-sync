package de.cronn.jira.sync.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.cronn.jira.sync.domain.Context;

public class TransitionConfig {

	private Set<String> sourceStatusIn = new LinkedHashSet<>();
	private Set<String> targetStatusIn = new LinkedHashSet<>();
	private String sourceStatusToSet;
	private String targetStatusToSet;
	private boolean copyResolutionToSource = false;
	private boolean copyFixVersionsToSource = false;
	private boolean onlyIfAssignedInTarget = false;
	private boolean assignToMyselfInSource = false;
	private boolean triggerIfIssueWasMovedBetweenProjects = false;
	private Context onlyIfStatusTransitionNewerIn = null;

	private Map<String, String> customFieldsToCopyFromTargetToSource = new LinkedHashMap<>();

	public TransitionConfig() {
	}

	public Set<String> getSourceStatusIn() {
		return sourceStatusIn;
	}

	public void setSourceStatusIn(Set<String> sourceStatusIn) {
		this.sourceStatusIn = sourceStatusIn;
	}

	public void setTargetStatusIn(Set<String> targetStatusIn) {
		this.targetStatusIn = targetStatusIn;
	}

	public String getSourceStatusToSet() {
		return sourceStatusToSet;
	}

	public void setSourceStatusToSet(String sourceStatusToSet) {
		this.sourceStatusToSet = sourceStatusToSet;
	}

	public String getTargetStatusToSet() {
		return targetStatusToSet;
	}

	public void setTargetStatusToSet(String targetStatusToSet) {
		this.targetStatusToSet = targetStatusToSet;
	}

	public boolean isCopyResolutionToSource() {
		return copyResolutionToSource;
	}

	public void setCopyResolutionToSource(boolean copyResolutionToSource) {
		this.copyResolutionToSource = copyResolutionToSource;
	}

	public boolean isCopyFixVersionsToSource() {
		return copyFixVersionsToSource;
	}

	public void setCopyFixVersionsToSource(boolean copyFixVersionsToSource) {
		this.copyFixVersionsToSource = copyFixVersionsToSource;
	}

	public void setOnlyIfAssignedInTarget(boolean onlyIfAssignedInTarget) {
		this.onlyIfAssignedInTarget = onlyIfAssignedInTarget;
	}

	public boolean isOnlyIfAssignedInTarget() {
		return onlyIfAssignedInTarget;
	}

	public void setAssignToMyselfInSource(boolean assignToMyselfInSource) {
		this.assignToMyselfInSource = assignToMyselfInSource;
	}

	public boolean isAssignToMyselfInSource() {
		return assignToMyselfInSource;
	}

	public Set<String> getTargetStatusIn() {
		return targetStatusIn;
	}

	public boolean isTriggerIfIssueWasMovedBetweenProjects() {
		return triggerIfIssueWasMovedBetweenProjects;
	}

	public void setTriggerIfIssueWasMovedBetweenProjects(boolean triggerIfIssueWasMovedBetweenProjects) {
		this.triggerIfIssueWasMovedBetweenProjects = triggerIfIssueWasMovedBetweenProjects;
	}

	public void setCustomFieldsToCopyFromTargetToSource(Map<String, String> customFieldsToCopyFromTargetToSource) {
		this.customFieldsToCopyFromTargetToSource = customFieldsToCopyFromTargetToSource;
	}

	public Map<String, String> getCustomFieldsToCopyFromTargetToSource() {
		return customFieldsToCopyFromTargetToSource;
	}
	
	public Context getOnlyIfStatusTransitionNewerIn() {
		return onlyIfStatusTransitionNewerIn;
	}

	public void setOnlyIfStatusTransitionNewerIn(Context onlyIfStatusTransitionNewerIn) {
		this.onlyIfStatusTransitionNewerIn = onlyIfStatusTransitionNewerIn;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("sourceStatusIn", sourceStatusIn)
			.append("targetStatusIn", targetStatusIn)
			.append("sourceStatusToSet", sourceStatusToSet)
			.append("targetStatusToSet", targetStatusToSet)
			.toString();
	}
}
