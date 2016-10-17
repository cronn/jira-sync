package de.cronn.jira.sync.domain;

import java.io.Serializable;

public class JiraLoginResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private JiraSession session;

	public JiraSession getSession() {
		return session;
	}

	public void setSession(JiraSession session) {
		this.session = session;
	}

}