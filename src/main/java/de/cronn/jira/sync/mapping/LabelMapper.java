package de.cronn.jira.sync.mapping;

import java.util.Set;

import de.cronn.jira.sync.domain.JiraIssue;

public interface LabelMapper {
	Set<String> mapLabels(JiraIssue sourceIssue);
}
