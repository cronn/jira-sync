package de.cronn.jira.sync.service;

import java.net.URL;

import de.cronn.jira.sync.JiraSyncException;

public class JiraResourceNotFoundException extends JiraSyncException {

	private static final long serialVersionUID = 1L;

	public JiraResourceNotFoundException(URL jiraUrl, String message) {
		super(jiraUrl, message);
	}
}
