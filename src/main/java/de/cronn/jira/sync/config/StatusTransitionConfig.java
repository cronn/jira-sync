package de.cronn.jira.sync.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class StatusTransitionConfig {

	private Set<String> sourceStatusIn;
	private Set<String> targetStatusIn;
	private String sourceStatusToSet;
	private boolean copyResolution = false;

	public StatusTransitionConfig() {
	}

	public StatusTransitionConfig(Collection<String> sourceStatusIn, Collection<String> targetStatusIn, String sourceStatusToSet) {
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

	public boolean isCopyResolution() {
		return copyResolution;
	}

	public void setCopyResolution(boolean copyResolution) {
		this.copyResolution = copyResolution;
	}

	public Set<String> getTargetStatusIn() {
		return targetStatusIn;
	}
}
