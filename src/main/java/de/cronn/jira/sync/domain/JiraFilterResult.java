package de.cronn.jira.sync.domain;

import java.io.Serializable;

public class JiraFilterResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private String jql;

	public String getJql() {
		return jql;
	}

	public void setJql(String jql) {
		this.jql = jql;
	}
}
