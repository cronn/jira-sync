package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class JiraFieldsUpdate implements Serializable {

	private static final long serialVersionUID = 1L;

	private String description;
	private JiraPriority priority;
	private JiraResolution resolution;
	private Set<String> labels;
	private Set<JiraVersion> versions;
	private Set<JiraVersion> fixVersions;
	private JiraUser assignee;
	private Map<String, Object> other = new LinkedHashMap<>();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	public Set<JiraVersion> getVersions() {
		return versions;
	}

	public void setVersions(Set<JiraVersion> versions) {
		this.versions = versions;
	}

	public Set<JiraVersion> getFixVersions() {
		return fixVersions;
	}

	public void setFixVersions(Set<JiraVersion> fixVersions) {
		this.fixVersions = fixVersions;
	}

	public JiraUser getAssignee() {
		return assignee;
	}

	public void setAssignee(JiraUser assignee) {
		this.assignee = assignee;
	}

	@JsonAnyGetter
	public Map<String, Object> getOther() {
		return other;
	}

	@JsonAnySetter
	public void setOther(String key, Object value) {
		other.put(key, value);
	}
}
