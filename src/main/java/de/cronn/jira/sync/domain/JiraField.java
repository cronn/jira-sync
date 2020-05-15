package de.cronn.jira.sync.domain;

public class JiraField extends JiraNamedResource {

	private static final long serialVersionUID = 2L;

	private Boolean custom;
	private JiraFieldSchema schema;

	public JiraField() {
	}

	public JiraField(String id, String name, boolean custom, JiraFieldSchema schema) {
		super(id, name);
		this.custom = custom;
		this.schema = schema;
	}

	public Boolean isCustom() {
		return custom;
	}

	public void setCustom(Boolean custom) {
		this.custom = custom;
	}

	public JiraFieldSchema getSchema() {
		return schema;
	}

	public void setSchema(JiraFieldSchema schema) {
		this.schema = schema;
	}

}
