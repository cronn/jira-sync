package de.cronn.jira.sync.domain;

import de.cronn.reflection.util.PropertyGetter;
import de.cronn.reflection.util.PropertyUtils;

public enum WellKnownJiraField {

	SUMMARY(JiraIssueFields::getSummary),
	STATUS(JiraIssueFields::getStatus),
	ISSUE_TYPE(JiraIssueFields::getIssuetype),
	DESCRIPTION(JiraIssueFields::getDescription),
	PRIORITY(JiraIssueFields::getPriority),
	PROJECT(JiraIssueFields::getProject),
	RESOLUTION(JiraIssueFields::getResolution),
	LABELS(JiraIssueFields::getLabels),
	VERSIONS(JiraIssueFields::getVersions),
	FIX_VERSIONS(JiraIssueFields::getFixVersions),
	COMPONENTS(JiraIssueFields::getComponents),
	ASSIGNEE(JiraIssueFields::getAssignee),
	UPDATED(JiraIssueFields::getUpdated),
	COMMENT(JiraIssueFields::getComment),
	;

	private final String fieldName;

	WellKnownJiraField(PropertyGetter<JiraIssueFields> getter) {
		this.fieldName = PropertyUtils.getPropertyName(JiraIssueFields.class, getter);
	}

	public String getFieldName() {
		return fieldName;
	}

}
