package de.cronn.jira.sync.service;

import de.cronn.jira.sync.JiraSyncException;

class JiraResourceNotFoundException extends JiraSyncException {

	private static final long serialVersionUID = 1L;

	JiraResourceNotFoundException(String jiraUrl, String message) {
		super(jiraUrl, message);
	}
}
