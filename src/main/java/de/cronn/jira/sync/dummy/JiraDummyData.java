package de.cronn.jira.sync.dummy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLinks;

public class JiraDummyData {

	private JiraLoginRequest credentials;
	private JiraIssueStatus defaultStatus;
	private Map<String, JiraIssue> issues = new LinkedHashMap<>();
	private Map<String, JiraRemoteLinks> remoteLinks = new LinkedHashMap<>();
	private Map<String, JiraProject> projects = new LinkedHashMap<>();
	private List<JiraPriority> priorities = new ArrayList<>();

	public JiraLoginRequest getCredentials() {
		return credentials;
	}

	public void setCredentials(JiraLoginRequest credentials) {
		Assert.isNull(this.credentials);
		this.credentials = credentials;
	}

	public void setDefaultStatus(JiraIssueStatus defaultStatus) {
		this.defaultStatus = defaultStatus;
	}

	public JiraIssueStatus getDefaultStatus() {
		return defaultStatus;
	}

	public List<JiraPriority> getPriorities() {
		return priorities;
	}

	public Map<String, JiraIssue> getIssues() {
		return issues;
	}

	public Map<String, JiraRemoteLinks> getRemoteLinks() {
		return remoteLinks;
	}

	public Map<String, JiraProject> getProjects() {
		return projects;
	}

}
