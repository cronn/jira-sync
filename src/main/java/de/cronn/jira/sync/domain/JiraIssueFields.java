package de.cronn.jira.sync.domain;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class JiraIssueFields {

	private JiraProject project;
	private String summary;
	private String description;
	private JiraIssueType issuetype;
	private JiraPriority priority;
	private JiraResolution resolution;
	private Set<String> labels;
	private JiraIssueStatus status;
	private Set<JiraVersion> versions;
	private Set<JiraVersion> fixVersions;

	public JiraIssueFields() {
	}

	public JiraIssueFields(JiraProject project) {
		this.project = project;
	}

	public JiraProject getProject() {
		return project;
	}

	public void setProject(JiraProject project) {
		this.project = project;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public JiraIssueType getIssuetype() {
		return issuetype;
	}

	public void setIssuetype(JiraIssueType issuetype) {
		this.issuetype = issuetype;
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	public JiraPriority getPriority() {
		return priority;
	}

	public void setPriority(JiraPriority priority) {
		this.priority = priority;
	}

	public JiraResolution getResolution() {
		return resolution;
	}

	public void setResolution(JiraResolution resolution) {
		this.resolution = resolution;
	}

	public JiraIssueStatus getStatus() {
		return status;
	}

	public void setStatus(JiraIssueStatus status) {
		this.status = status;
	}

	public void setVersions(Set<JiraVersion> versions) {
		this.versions = versions;
	}

	public Set<JiraVersion> getVersions() {
		return versions;
	}

	public void setFixVersions(Set<JiraVersion> fixVersions) {
		this.fixVersions = fixVersions;
	}

	public Set<JiraVersion> getFixVersions() {
		return fixVersions;
	}
}
