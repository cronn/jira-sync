package de.cronn.jira.sync.domain;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraProject extends JiraIdResource {

	private String key;
	private String name;
	private String description;
	private List<JiraIssueType> issueTypes;

	public JiraProject() {
	}

	public JiraProject(String id, String key) {
		super(id);
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setIssueTypes(List<JiraIssueType> issueTypes) {
		this.issueTypes = issueTypes;
	}

	public List<JiraIssueType> getIssueTypes() {
		return issueTypes;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("key", key)
			.append("name", name)
			.append("description", description)
			.toString();
	}
}
