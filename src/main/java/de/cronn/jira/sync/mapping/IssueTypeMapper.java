package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraProject;

public interface IssueTypeMapper {
	JiraIssueType mapIssueType(JiraIssue sourceIssue, JiraSyncConfig syncConfig, JiraProjectSync projectSync, JiraProject targetProject);
}
