package de.cronn.jira.sync.domain;

import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class JiraFieldsUpdate {

	private String summary;
	private String description;
	private JiraPriority priority;
	private JiraResolution resolution;
	private Set<String> labels;
	private Set<JiraVersion> versions;
	private Set<JiraVersion> fixVersions;
	private JiraUser assignee;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		JiraFieldsUpdate that = (JiraFieldsUpdate) o;

		return new EqualsBuilder()
			.append(summary, that.summary)
			.append(description, that.description)
			.append(priority, that.priority)
			.append(resolution, that.resolution)
			.append(labels, that.labels)
			.append(versions, that.versions)
			.append(fixVersions, that.fixVersions)
			.append(assignee, that.assignee)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(summary)
			.append(description)
			.append(priority)
			.append(resolution)
			.append(labels)
			.append(versions)
			.append(fixVersions)
			.append(assignee)
			.toHashCode();
	}
}
