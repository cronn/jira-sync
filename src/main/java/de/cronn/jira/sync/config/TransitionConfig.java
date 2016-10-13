package de.cronn.jira.sync.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class TransitionConfig {

	private Set<String> sourceStatusIn;
	private Set<String> targetStatusIn;
	private String sourceStatusToSet;
	private boolean copyResolutionToSource = false;
	private boolean copyFixVersionsToSource = false;
	private boolean onlyIfAssignedInTarget = false;
	private boolean assignToMyselfInSource = false;

	public TransitionConfig() {
	}

	public TransitionConfig(Collection<String> sourceStatusIn, Collection<String> targetStatusIn, String sourceStatusToSet) {
		this.sourceStatusIn = new LinkedHashSet<>(sourceStatusIn);
		this.targetStatusIn = new LinkedHashSet<>(targetStatusIn);
		this.sourceStatusToSet = sourceStatusToSet;
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
}
