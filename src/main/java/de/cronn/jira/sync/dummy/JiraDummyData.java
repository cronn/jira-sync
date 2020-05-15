package de.cronn.jira.sync.dummy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.Assert;

import de.cronn.jira.sync.domain.JiraComponent;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLinks;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;

public class JiraDummyData {

	private JiraLoginRequest credentials;
	private JiraIssueStatus defaultStatus;
	private final Map<String, JiraFilter> filters = new LinkedHashMap<>();
	private final Map<String, JiraIssue> issues = new LinkedHashMap<>();
	private final Map<String, JiraRemoteLinks> remoteLinks = new LinkedHashMap<>();
	private final Map<String, JiraProject> projects = new LinkedHashMap<>();
	private final List<JiraPriority> priorities = new ArrayList<>();
	private final List<JiraResolution> resolutions = new ArrayList<>();
	private final List<JiraTransition> transitions = new ArrayList<>();
	private final List<JiraVersion> versions = new ArrayList<>();
	private final List<JiraComponent> components = new ArrayList<>();
	private final Map<JiraField, Map<String, Long>> customFields = new LinkedHashMap<>();
	private final Map<String, AtomicLong> keyCounters = new LinkedHashMap<>();
	private final AtomicLong idCounter = new AtomicLong();
	private final Map<String, JiraUser> users = new LinkedHashMap<>();
	private String baseUrl;
	private BasicAuthCredentials basicAuthCredentials;
	private JiraPriority defaultPriority;

	public JiraLoginRequest getCredentials() {
		return credentials;
	}

	public void setCredentials(JiraLoginRequest credentials) {
		Assert.isNull(this.credentials, "credentials already set");
		this.credentials = credentials;
	}

	public List<JiraTransition> getTransitions() {
		return transitions;
	}

	public List<JiraVersion> getVersions() {
		return versions;
	}

	public List<JiraComponent> getComponents() {
		return components;
	}

	public Map<JiraField, Map<String, Long>> getCustomFields() {
		return customFields;
	}

	public List<JiraResolution> getResolutions() {
		return resolutions;
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

	public JiraPriority getDefaultPriority() {
		return defaultPriority;
	}

	public void setDefaultPriority(JiraPriority defaultPriority) {
		this.defaultPriority = defaultPriority;
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

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public Map<String, JiraFilter> getFilters() {
		return filters;
	}

	public void setBasicAuthCredentials(String username, String password) {
		basicAuthCredentials = new BasicAuthCredentials(username, password);
	}

	public BasicAuthCredentials getBasicAuthCredentials() {
		return basicAuthCredentials;
	}

	public AtomicLong getOrCreateKeyCounter(String projectKey) {
		return keyCounters.computeIfAbsent(projectKey, k -> new AtomicLong());
	}

	public AtomicLong getIdCounter() {
		return idCounter;
	}

	public void addField(JiraField field, Map<String, Long> allowedValues) {
		customFields.put(field, allowedValues);
	}

	public void addUser(JiraUser user) {
		Object old = users.putIfAbsent(user.getName(), user);
		Assert.isNull(old, "user " + user.getName() + " already exists");
	}

	public JiraUser getUser(String username) {
		return users.get(username);
	}
}
