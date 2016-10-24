package de.cronn.jira.sync.mapping;

import java.util.Map;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface CustomFieldMapper {

	Map<String, Object> map(JiraIssue fromIssue, JiraService fromJira, JiraService toJira);
}
