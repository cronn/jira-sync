package de.cronn.jira.sync.strategy;

import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface MissingTargetJiraIssueSyncStrategy extends IssueSyncStrategy {

	@Override
	default SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue targetIssue, JiraProjectSync projectSync) {
		Assert.isNull(targetIssue, "targetIssue most not be null");
		return sync(jiraSource, jiraTarget, sourceIssue, projectSync);
	}

	SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraProjectSync projectSync);

}
