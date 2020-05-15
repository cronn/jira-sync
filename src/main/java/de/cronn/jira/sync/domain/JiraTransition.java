package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class JiraTransition extends JiraNamedResource {

	private static final long serialVersionUID = 1L;

	private JiraIssueStatus to;

	public JiraTransition() {
	}

	public JiraTransition(String id, String name, JiraIssueStatus to) {
		super(id, name);
		this.to = to;
	}

	public JiraIssueStatus getTo() {
		return to;
	}

	public void setTo(JiraIssueStatus to) {
		this.to = to;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("name", getName())
			.append("to", to)
			.toString();
	}
}
