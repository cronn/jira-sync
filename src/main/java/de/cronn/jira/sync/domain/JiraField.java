
package de.cronn.jira.sync.domain;

public enum JiraField {

	SUMMARY("summary"),
	STATUS("status"),
	ISSUETYPE("issuetype"),
	DESCRIPTION("description"),
	PRIORITY("priority"),
	PROJECT("project"),
	RESOLUTION("resolution"),
	LABELS("labels"),
	VERSIONS("versions"),
	FIX_VERSIONS("fixVersions"),
	ASSIGNEE("assignee");

	private final String name;

	JiraField(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
