package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.service.JiraService;

public interface UsernameReplacer {

	String replaceUsernames(String inputText, JiraService jiraService);

}
