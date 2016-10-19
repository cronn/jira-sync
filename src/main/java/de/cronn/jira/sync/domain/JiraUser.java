package de.cronn.jira.sync.domain;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraUser extends JiraResource implements Serializable {

	private static final long serialVersionUID = 2L;

	private String name;
	private String key;
	private String displayName;

	public JiraUser() {
	}

	public JiraUser(String name, String key) {
		this.name = name;
		this.key = key;
	}

	public JiraUser(String name, String key, String displayName) {
		this.name = name;
		this.key = key;
		this.displayName = displayName;
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("key", key)
			.append("name", name)
			.append("displayName", displayName)
			.toString();
	}
}
