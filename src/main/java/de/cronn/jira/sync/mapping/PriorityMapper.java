package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.service.JiraService;

public interface PriorityMapper {

	JiraPriority mapPriority(JiraService jiraTarget, JiraIssue sourceIssue);

}
