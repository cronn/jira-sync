package de.cronn.jira.sync.strategy;

public enum SyncResult {

	CREATED("created"),
	UNCHANGED("unchanged"),
	UNCHANGED_WARNING("warning (unchanged)"),
	CHANGED("changed"),
	CHANGED_TRANSITION("changed (transition)"),
	;

	private final String displayName;

	SyncResult(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
