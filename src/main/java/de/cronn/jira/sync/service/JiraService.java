package de.cronn.jira.sync.service;

import java.net.URL;
import java.util.List;

import de.cronn.jira.sync.config.JiraConnectionProperties;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;

public interface JiraService extends AutoCloseable {

	URL getUrl();

	void login(JiraConnectionProperties connectionProperties);

	void logout();

	@Override
	void close();

	JiraServerInfo getServerInfo();

	JiraUser getMyself();

	JiraIssue getIssueByKey(String key);

	JiraProject getProjectByKey(String projectKey);

	List<JiraPriority> getPriorities();

	List<JiraResolution> getResolutions();

	List<JiraVersion> getVersions(String projectKey);

	List<JiraIssue> getIssuesByFilterId(String filterId);

	List<JiraRemoteLink> getRemoteLinks(JiraIssue issue);

	List<JiraTransition> getTransitions(JiraIssue issue);

	void addRemoteLink(JiraIssue fromIssue, JiraIssue toIssue, JiraService toJiraService, URL remoteLinkIcon);

	JiraIssue createIssue(JiraIssue issue);

	void updateIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate);

	void transitionIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate);
}
