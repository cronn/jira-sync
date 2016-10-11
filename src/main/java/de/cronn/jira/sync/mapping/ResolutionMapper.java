package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.service.JiraService;

public interface ResolutionMapper {
	JiraResolution mapResolution(JiraService jiraService, JiraIssue jiraIssue);
}
