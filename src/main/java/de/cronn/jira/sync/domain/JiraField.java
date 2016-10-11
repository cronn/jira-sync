
package de.cronn.jira.sync.domain;

public enum JiraField {

	SUMMARY("summary"),
	STATUS("status"),
	ISSUETYPE("issuetype"),
	DESCRIPTION("description"),
	PRIORITY("priority"),
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

	public static JiraField forName(String name) {
		for (JiraField field : values()) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Field not found for name '" + name + "'");
	}
}
