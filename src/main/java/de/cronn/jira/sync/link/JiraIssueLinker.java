package de.cronn.jira.sync.link;

import java.net.URL;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface JiraIssueLinker {

	JiraIssue resolveIssue(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService);

	String resolveKey(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService);

	void linkIssue(JiraIssue fromIssue, JiraIssue toIssue, JiraService fromJiraService, JiraService toJiraService, URL iconUrl);

}
