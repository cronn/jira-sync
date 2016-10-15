package de.cronn.jira.sync.link;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface JiraIssueLinker {

	JiraIssue resolve(JiraIssue sourceIssue, JiraService jiraSource, JiraService jiraTarget);

	void linkIssues(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget, JiraProjectSync projectSync);

}
