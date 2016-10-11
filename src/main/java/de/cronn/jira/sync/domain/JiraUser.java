package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraUser extends JiraResource {

	private String name;
	private String key;

	public JiraUser() {
	}

	public JiraUser(String name, String key) {
		this.name = name;
		this.key = key;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("name", name)
			.append("key", key)
			.toString();
	}
}
