package de.cronn.jira.sync.domain;

import java.util.Set;

public interface JiraFieldsBean {

	String getDescription();

	JiraUser getAssignee();

	JiraResolution getResolution();

	void setDescription(String description);

	void setAssignee(JiraUser assignee);

	void setResolution(JiraResolution resolution);

	Set<JiraVersion> getVersions();

	void setVersions(Set<JiraVersion> versions);

	Set<JiraVersion> getFixVersions();

	void setFixVersions(Set<JiraVersion> fixVersions);

	Set<JiraComponent> getComponents();

	void setComponents(Set<JiraComponent> components);

}
