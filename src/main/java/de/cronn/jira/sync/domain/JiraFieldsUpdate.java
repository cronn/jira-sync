package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraFieldsUpdate implements Serializable, JiraFieldsBean {

	private static final long serialVersionUID = 1L;

	private String description;
	private JiraPriority priority;
	private JiraResolution resolution;
	private Set<String> labels;
	private Set<JiraVersion> versions;
	private Set<JiraVersion> fixVersions;
	private Set<JiraComponent> components;
	private JiraUser assignee;
	private Map<String, Object> other = new LinkedHashMap<>();

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@JsonIgnore
	public JiraFieldsUpdate withDescription(String description) {
		setDescription(description);
		return this;
	}

	public JiraPriority getPriority() {
		return priority;
	}

	public void setPriority(JiraPriority priority) {
		this.priority = priority;
	}

	@Override
	public JiraResolution getResolution() {
		return resolution;
	}

	@Override
	public void setResolution(JiraResolution resolution) {
		this.resolution = resolution;
	}

	public JiraFieldsUpdate withResolution(JiraResolution resolution) {
		this.resolution = resolution;
		return this;
	}

	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	@Override
	public Set<JiraVersion> getVersions() {
		return versions;
	}

	@Override
	public void setVersions(Set<JiraVersion> versions) {
		this.versions = versions;
	}

	@JsonIgnore
	public JiraFieldsUpdate withVersions(Set<JiraVersion> jiraVersions) {
		setVersions(jiraVersions);
		return this;
	}

	@Override
	public Set<JiraVersion> getFixVersions() {
		return fixVersions;
	}

	@Override
	public void setFixVersions(Set<JiraVersion> fixVersions) {
		this.fixVersions = fixVersions;
	}

	@JsonIgnore
	public JiraFieldsUpdate withFixVersions(Set<JiraVersion> jiraVersions) {
		setFixVersions(jiraVersions);
		return this;
	}

	@Override
	public Set<JiraComponent> getComponents() {
		return components;
	}

	@Override
	public void setComponents(Set<JiraComponent> components) {
		this.components = components;
	}

	@JsonIgnore
	public JiraFieldsUpdate withComponents(Set<JiraComponent> jiraComponents) {
		setComponents(jiraComponents);
		return this;
	}

	@Override
	public JiraUser getAssignee() {
		return assignee;
	}

	@Override
	public void setAssignee(JiraUser assignee) {
		this.assignee = assignee;
	}

	@JsonIgnore
	public JiraFieldsUpdate withAssignee(JiraUser jiraUser) {
		setAssignee(jiraUser);
		return this;
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
