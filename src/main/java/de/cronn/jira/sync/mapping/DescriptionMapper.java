package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraIssue;

public interface DescriptionMapper {

	String mapSourceDescription(JiraIssue sourceIssue);

	String getDescription(JiraIssue sourceIssue);

	String mapTargetDescription(JiraIssue sourceIssue, JiraIssue targetIssue);
}
