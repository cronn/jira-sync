package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface DescriptionMapper {

	String mapSourceDescription(JiraIssue sourceIssue, JiraService jiraSource);

	String getDescription(JiraIssue sourceIssue);

	String mapTargetDescription(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource);
}
