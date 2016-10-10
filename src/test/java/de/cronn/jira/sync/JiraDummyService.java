package de.cronn.jira.sync;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.cronn.jira.sync.config.JiraConnectionProperties;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraRemoteLinks;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraTransitions;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@Component
@Primary
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class JiraDummyService implements JiraService {

	private static final Logger log = LoggerFactory.getLogger(JiraDummyService.class);

	private URL url;

	private static final Map<URL, JiraDummyData> DUMMY_DATA = new LinkedHashMap<>();

	static void reset() {
		DUMMY_DATA.clear();
	}

	void setUrl(URL url) {
		assertNotNull(url);
		this.url = url;
	}

	void expectLoginRequest(String username, String password) throws Exception {
		getDummyData().setCredentials(new JiraLoginRequest(username, password));
	}

	void setDefaultStatus(JiraIssueStatus status) {
		getDummyData().setDefaultStatus(status);
	}

	void addProject(JiraProject jiraProject) {
		assertNotNull(jiraProject.getKey());
		Object old = getProjects().put(jiraProject.getKey(), jiraProject);
		assertNull(old);
	}

	void addPriority(JiraPriority jiraPriority) {
		getDummyData().getPriorities().add(jiraPriority);
	}

	private Map<String, JiraProject> getProjects() {
		return getDummyData().getProjects();
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public void login(JiraConnectionProperties connectionProperties) {
		this.url = connectionProperties.getUrl();
		JiraLoginRequest credentials = getDummyData().getCredentials();
		assertNotNull("Expected login not configured for " + url, credentials);
		assertEquals(connectionProperties.getUsername(), credentials.getUsername());
		assertEquals(connectionProperties.getPassword(), credentials.getPassword());
	}

	@Override
	public void logout() {
		log.debug("[{}] logout", getUrl());
		url = null;
	}

	@Override
	public void close() {
		log.debug("closing");
	}

	@Override
	public JiraServerInfo getServerInfo() {
		JiraServerInfo jiraServerInfo = new JiraServerInfo(url.toString());
		jiraServerInfo.setServerTitle(url.getHost());
		return jiraServerInfo;
	}

	@Override
	public JiraIssue getIssueByKey(String key) {
		JiraIssue issue = getIssueMap().get(key);
		assertNotNull("Issue " + key + " not found", issue);
		return issue;
	}

	@Override
	public JiraProject getProjectByKey(String projectKey) {
		JiraProject jiraProject = getProjects().get(projectKey);
		assertNotNull("Project " + projectKey + " not found", jiraProject);
		return jiraProject;
	}

	@Override
	public List<JiraPriority> getPriorities() {
		return getDummyData().getPriorities();
	}

	@Override
	public List<JiraResolution> getResolutions() {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public List<JiraVersion> getVersions(String projectKey) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public List<JiraIssue> getIssuesByFilterId(String filterId) {
		return getAllIssues();
	}

	List<JiraIssue> getAllIssues() {
		Map<String, JiraIssue> issuesPerKey = getIssueMap();
		return Collections.unmodifiableList(new ArrayList<>(issuesPerKey.values()));
	}

	private Map<String, JiraIssue> getIssueMap() {
		return getDummyData().getIssues();
	}

	private JiraDummyData getDummyData() {
		assertNotNull(url);
		return DUMMY_DATA.computeIfAbsent(url, k -> new JiraDummyData());
	}

	@Override
	public List<JiraRemoteLink> getRemoteLinks(JiraIssue issue) {
		assertNotNull(issue.getKey());
		Map<String, JiraRemoteLinks> remoteLinksPerIssueKey = getDummyData().getRemoteLinks();
		JiraRemoteLinks jiraRemoteLinks = remoteLinksPerIssueKey.get(issue.getKey());
		if (jiraRemoteLinks == null) {
			return Collections.emptyList();
		}
		return jiraRemoteLinks;
	}

	@Override
	public JiraTransitions getTransitions(JiraIssue issue) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void addRemoteLink(JiraIssue fromIssue, JiraIssue toIssue, JiraService toJiraService, URL remoteLinkIcon) {
		JiraRemoteLinks remoteLinks = getDummyData().getRemoteLinks().computeIfAbsent(fromIssue.getKey(), k -> new JiraRemoteLinks());
		remoteLinks.add(new JiraRemoteLink(toJiraService.getUrl() + "/browse/" + toIssue.getKey()));
	}

	@Override
	public JiraIssue createIssue(JiraIssue issue) {
		JiraProject project = issue.getFields().getProject();
		assertNotNull(project);
		if (issue.getKey() == null) {
			long id = getIssueMap().size() + 1;
			String projectKey = project.getKey();
			assertNotNull(projectKey);
			issue.setKey(projectKey + "-" + id);
			issue.setId(String.valueOf(id));
		}
		if (issue.getFields().getStatus() == null) {
			JiraIssueStatus defaultStatus = getDummyData().getDefaultStatus();
			assertNotNull("defaultStatus must be set", defaultStatus);
			issue.getFields().setStatus(defaultStatus);
		}
		Object old = getIssueMap().put(issue.getKey(), issue);
		assertNull(old);
		return issue;
	}

	@Override
	public void updateIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueByKey(issue.getKey());
		assertNull(jiraIssueUpdate.getTransition());
		for (Map.Entry<String, Object> entry : jiraIssueUpdate.getFields().entrySet()) {
			switch (entry.getKey()) {
				case "description":
					issueInSystem.getFields().setDescription((String) entry.getValue());
					break;
				default:
					fail("unsupported field update: " + entry.getKey());
			}
		}
	}

	@Override
	public void transitionIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("url", url)
			.toString();
	}
}
