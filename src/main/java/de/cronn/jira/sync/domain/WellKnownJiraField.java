package de.cronn.jira.sync.domain;

public enum WellKnownJiraField {

	SUMMARY("summary"),
	STATUS("status"),
	ISSUE_TYPE("issuetype"),
	DESCRIPTION("description"),
	PRIORITY("priority"),
	PROJECT("project"),
	RESOLUTION("resolution"),
	LABELS("labels"),
	VERSIONS("versions"),
	FIX_VERSIONS("fixVersions"),
	ASSIGNEE("assignee"),
	UPDATED("updated"),
	COMMENT("comment"),
	;

	private final String fieldName;

	WellKnownJiraField(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

}
