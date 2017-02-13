package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraField extends JiraIdResource {

	private static final long serialVersionUID = 1L;

	private String name;
	private Boolean custom;

	public JiraField() {
	}

	public JiraField(String id, String name, boolean custom) {
		super(id);
		this.name = name;
		this.custom = custom;
	}

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

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("name", getName())
			.toString();
	}


}