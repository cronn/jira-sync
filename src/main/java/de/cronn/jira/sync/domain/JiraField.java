package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraField extends JiraIdResource implements JiraNamedBean {

	private static final long serialVersionUID = 2L;

	private String name;
	private Boolean custom;
	private JiraFieldSchema schema;

	public JiraField() {
	}

	public JiraField(String id, String name, boolean custom, JiraFieldSchema schema) {
		super(id);
		this.name = name;
		this.custom = custom;
		this.schema = schema;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("name", getName())
			.toString();
	}

}
