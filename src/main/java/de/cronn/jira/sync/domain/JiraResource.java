package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class JiraResource {

	private String self;

	protected JiraResource() {
	}

	public void setSelf(String self) {
		this.self = self;
	}

	public String getSelf() {
		return self;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("self", self)
			.toString();
	}
}
