package de.cronn.jira.sync.mapping;

import java.util.Map;

import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.service.JiraService;

public interface FieldMapper {

	Map<String, Object> map(JiraIssue fromIssue, JiraService fromJira, JiraService toJira, JiraProject toProject);

	Object mapValue(JiraIssue fromIssue, JiraField fromField, JiraField toField, JiraService toJira, JiraProject toProject);
}
