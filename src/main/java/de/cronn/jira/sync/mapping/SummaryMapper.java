package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraIssue;

public interface SummaryMapper {

	String mapSummary(JiraIssue sourceIssue);

}
