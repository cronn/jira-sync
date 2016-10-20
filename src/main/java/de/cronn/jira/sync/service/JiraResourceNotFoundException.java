package de.cronn.jira.sync.service;

import de.cronn.jira.sync.JiraSyncException;

public class JiraResourceNotFoundException extends JiraSyncException {

	private static final long serialVersionUID = 1L;

	public JiraResourceNotFoundException(String message) {
		super(message);
	}
}
