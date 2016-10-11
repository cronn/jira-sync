package de.cronn.jira.sync.dummy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraConnectionProperties;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraLinkIcon;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraRemoteLinks;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

public class JiraDummyService implements JiraService {

	private static final Logger log = LoggerFactory.getLogger(JiraDummyService.class);

	private URL url;

	private static final Map<String, JiraDummyData> DUMMY_DATA = new LinkedHashMap<>();

	public static void reset() {
		DUMMY_DATA.clear();
	}

	public void setUrl(URL url) {
		Assert.notNull(url);
		this.url = url;
	}

	public void expectLoginRequest(String username, String password) throws Exception {
		getDummyData().setCredentials(new JiraLoginRequest(username, password));
	}

	public void setDefaultStatus(JiraIssueStatus status) {
		getDummyData().setDefaultStatus(status);
	}

	public void addProject(JiraProject project) {
		Assert.notNull(project.getKey());
		Object old = getProjects().put(project.getKey(), project);
		Assert.isNull(old);
	}

	public void addTransition(JiraTransition transition) {
		List<JiraTransition> transitions = getDummyData().getTransitions();
		transitions.add(transition);
	}

	public void addPriority(JiraPriority priority) {
		getDummyData().getPriorities().add(priority);
	}

	public void addResolution(JiraResolution resolution) {
		getDummyData().getResolutions().add(resolution);
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
		Assert.notNull(credentials, "Expected login not configured for " + url);
		Assert.state(Objects.equals(connectionProperties.getUsername(), credentials.getUsername()));
		Assert.state(Objects.equals(connectionProperties.getPassword(), credentials.getPassword()));
	}

	@Override
	public JiraUser getMyself() {
		return new JiraUser("me", "myself");
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
		Assert.notNull(issue, "Issue " + key + " not found");
		return issue;
	}

	@Override
	public JiraProject getProjectByKey(String projectKey) {
		JiraProject jiraProject = getProjects().get(projectKey);
		Assert.notNull(jiraProject, "Project " + projectKey + " not found");
		return jiraProject;
	}

	@Override
	public List<JiraPriority> getPriorities() {
		return getDummyData().getPriorities();
	}

	@Override
	public List<JiraResolution> getResolutions() {
		return getDummyData().getResolutions();
	}

	@Override
	public List<JiraVersion> getVersions(String projectKey) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public List<JiraIssue> getIssuesByFilterId(String filterId) {
		return getAllIssues();
	}

	public List<JiraIssue> getAllIssues() {
		Map<String, JiraIssue> issuesPerKey = getIssueMap();
		return Collections.unmodifiableList(new ArrayList<>(issuesPerKey.values()));
	}

	private Map<String, JiraIssue> getIssueMap() {
		return getDummyData().getIssues();
	}

	private JiraDummyData getDummyData() {
		Assert.notNull(url);
		return DUMMY_DATA.computeIfAbsent(url.toString(), k -> new JiraDummyData());
	}

	@Override
	public List<JiraRemoteLink> getRemoteLinks(JiraIssue issue) {
		Assert.notNull(issue.getKey());
		Map<String, JiraRemoteLinks> remoteLinksPerIssueKey = getDummyData().getRemoteLinks();
		JiraRemoteLinks jiraRemoteLinks = remoteLinksPerIssueKey.get(issue.getKey());
		if (jiraRemoteLinks == null) {
			return Collections.emptyList();
		}
		return jiraRemoteLinks;
	}

	@Override
	public List<JiraTransition> getTransitions(JiraIssue issue) {
		return getDummyData().getTransitions();
	}

	@Override
	public void addRemoteLink(JiraIssue fromIssue, JiraIssue toIssue, JiraService toJiraService, URL remoteLinkIcon) {
		JiraRemoteLinks remoteLinks = getDummyData().getRemoteLinks().computeIfAbsent(fromIssue.getKey(), k -> new JiraRemoteLinks());
		String url = toJiraService.getUrl().toString();
		JiraRemoteLink jiraRemoteLink = new JiraRemoteLink(url + (url.endsWith("/") ? "" : "/") + "browse/" + toIssue.getKey());
		jiraRemoteLink.getObject().setIcon(new JiraLinkIcon(remoteLinkIcon));
		remoteLinks.add(jiraRemoteLink);
	}

	@Override
	public JiraIssue createIssue(JiraIssue issue) {
		JiraProject project = issue.getFields().getProject();
		Assert.notNull(project);
		if (issue.getKey() == null) {
			long id = getIssueMap().size() + 1;
			String projectKey = project.getKey();
			Assert.notNull(projectKey);
			issue.setKey(projectKey + "-" + id);
			issue.setId(String.valueOf(id));
		}
		if (issue.getFields().getStatus() == null) {
			JiraIssueStatus defaultStatus = getDummyData().getDefaultStatus();
			Assert.notNull(defaultStatus, "defaultStatus must be set");
			issue.getFields().setStatus(defaultStatus);
		}
		Object old = getIssueMap().put(issue.getKey(), issue);
		Assert.isNull(old);
		return issue;
	}

	@Override
	public void updateIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueByKey(issue.getKey());
		Assert.isNull(jiraIssueUpdate.getTransition());
		updateFields(jiraIssueUpdate, issueInSystem);
	}

	private void updateFields(JiraIssueUpdate jiraIssueUpdate, JiraIssue issueInSystem) {
		Map<String, Object> fields = jiraIssueUpdate.getFields();
		if (fields == null) {
			return;
		}
		for (Map.Entry<String, Object> entry : fields.entrySet()) {
			updateField(issueInSystem, entry.getKey(), entry.getValue());
		}
	}

	private void updateField(JiraIssue issueInSystem, String key, Object value) {
		switch (JiraField.forName(key)) {
			case DESCRIPTION:
				issueInSystem.getFields().setDescription((String) value);
				break;
			case RESOLUTION:
				issueInSystem.getFields().setResolution((JiraResolution) value);
				break;
			case ASSIGNEE:
				issueInSystem.getFields().setAssignee((JiraUser) value);
				break;
			default:
				throw new IllegalArgumentException("unsupported field update: " + key);
		}
	}

	@Override
	public void transitionIssue(JiraIssue issue, JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueByKey(issue.getKey());
		Assert.notNull(jiraIssueUpdate.getTransition());

		JiraIssueStatus targetStatus = jiraIssueUpdate.getTransition().getTo();
		Assert.notNull(targetStatus);
		log.debug("Updating status of {} to {}", issue, targetStatus);
		issueInSystem.getFields().setStatus(targetStatus);

		updateFields(jiraIssueUpdate, issueInSystem);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("url", url)
			.toString();
	}
}
