package de.cronn.jira.sync.domain;

public enum WellKnownCustomFieldType {

	LABELS("com.atlassian.jira.plugin.system.customfieldtypes:labels"),
	TEXTAREA("com.atlassian.jira.plugin.system.customfieldtypes:textarea"),
	SELECT("com.atlassian.jira.plugin.system.customfieldtypes:select"),
	;

	private final String schemaType;

	WellKnownCustomFieldType(String schemaType) {
		this.schemaType = schemaType;
	}

	public String getSchemaType() {
		return schemaType;
	}

	public static WellKnownCustomFieldType getByCustomSchema(JiraFieldSchema fieldSchema) {
		String customSchema = fieldSchema.getCustom();
		for (WellKnownCustomFieldType wellKnownCustomFieldType : values()) {
			if (wellKnownCustomFieldType.getSchemaType().equals(customSchema)) {
				return wellKnownCustomFieldType;
			}
		}
		throw new IllegalArgumentException("Unknown schema: " + customSchema);
	}

}
