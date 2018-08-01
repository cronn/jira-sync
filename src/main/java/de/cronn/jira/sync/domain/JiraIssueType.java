package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraIssueType extends JiraIdResource implements JiraNamedBean {

	private static final long serialVersionUID = 1L;

	private String name;

	public JiraIssueType() {
	}

	public JiraIssueType(String id, String name) {
		super(id);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("name", name)
			.toString();
	}
}
