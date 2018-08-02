package de.cronn.jira.sync.domain;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraIssueStatus extends JiraIdResource implements JiraNamedBean {

	private static final long serialVersionUID = 1L;

	private String name;

	public JiraIssueStatus() {
	}

	public JiraIssueStatus(String id, String name) {
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
			.append("name", getName())
			.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JiraIssueStatus that = (JiraIssueStatus) o;
		return Objects.equals(getId(), that.getId())
			&& Objects.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getName());
	}
}
