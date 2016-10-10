package de.cronn.jira.sync.resolve;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface JiraIssueResolver {

	JiraIssue resolve(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService);
}
