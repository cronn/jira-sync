package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.service.JiraService;

public interface TicketReferenceReplacer {

	String replaceTicketReferences(String inputText, JiraService jiraService);

}
