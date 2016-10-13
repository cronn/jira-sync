package de.cronn.jira.sync.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.cronn.jira.sync.domain.JiraFieldsUpdate;
import de.cronn.jira.sync.domain.JiraFilterResult;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraLoginResponse;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraRemoteLinks;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraSearchResult;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraSession;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraTransitions;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;

@RestController
@RequestMapping("/{" + JiraDummyService.CONTEXT + "}/rest")
public class JiraDummyService {

	private static final Logger log = LoggerFactory.getLogger(JiraDummyService.class);

	public static final String CONTEXT = "context";

	private final Map<Context, JiraDummyData> data = new EnumMap<>(Context.class);

	public void reset() {
		data.clear();
	}

	public enum Context {
		SOURCE, TARGET;
	}

	public void setBaseUrl(Context context, String baseUrl) {
		getData(context).setBaseUrl(baseUrl);
	}

	public void expectLoginRequest(Context context, String username, String password) throws Exception {
		getData(context).setCredentials(new JiraLoginRequest(username, password));
	}

	public void setDefaultStatus(Context context, JiraIssueStatus status) {
		getData(context).setDefaultStatus(status);
	}

	public void addProject(Context context, JiraProject project) {
		Assert.notNull(project.getKey());
		Assert.notNull(project.getId());
		Object old = getProjects(context).put(project.getKey(), project);
		Assert.isNull(old);
	}

	public void addTransition(Context context, JiraTransition transition) {
		List<JiraTransition> transitions = getData(context).getTransitions();
		transitions.add(transition);
	}

	public void addVersion(Context context, JiraVersion version) {
		List<JiraVersion> versions = getData(context).getVersions();
		versions.add(version);
	}

	public void addPriority(Context context, JiraPriority priority) {
		getData(context).getPriorities().add(priority);
	}

	public void addResolution(Context context, JiraResolution resolution) {
		getData(context).getResolutions().add(resolution);
	}

	private Map<String, JiraProject> getProjects(Context context) {
		return getData(context).getProjects();
	}

	@RequestMapping(path = "/api/2/serverInfo", method = RequestMethod.GET)
	public JiraServerInfo serverInfo(@PathVariable(CONTEXT) Context context) {
		JiraServerInfo jiraServerInfo = new JiraServerInfo(getData(context).getBaseUrl());
		jiraServerInfo.setServerTitle(context + " Jira");
		jiraServerInfo.setVersion("DummyService");
		return jiraServerInfo;
	}

	@RequestMapping(path = "/api/2/filter/{filterId}", method = RequestMethod.GET)
	public JiraFilterResult filter(@PathVariable(CONTEXT) Context context, @PathVariable("filterId") String filterId) {
		JiraFilterResult result = new JiraFilterResult();
		result.setJql("dummy");
		return result;
	}

	@RequestMapping(path = "/api/2/search", method = RequestMethod.GET)
	public JiraSearchResult search(@PathVariable(CONTEXT) Context context) {
		JiraSearchResult result = new JiraSearchResult();
		List<JiraIssue> allIssues = getAllIssues(context);
		result.setIssues(allIssues);
		result.setMaxResults(allIssues.size());
		result.setTotal(allIssues.size());
		return result;
	}

	@RequestMapping(path = "/auth/1/session", method = RequestMethod.POST)
	public JiraLoginResponse login(@PathVariable(CONTEXT) Context context, @RequestBody JiraLoginRequest loginRequest) {
		JiraLoginRequest credentials = getData(context).getCredentials();
		if (!credentials.getUsername().equals(loginRequest.getUsername())) {
			throw new IllegalArgumentException("Illegal username");
		}
		if (!credentials.getPassword().equals(loginRequest.getPassword())) {
			throw new IllegalArgumentException("Illegal password");
		}
		JiraLoginResponse loginResponse = new JiraLoginResponse();
		JiraSession session = new JiraSession();
		session.setName("test-session-" + context);
		session.setValue("test-session-" + context);
		loginResponse.setSession(session);

		log.debug("[{}] login", context);

		return loginResponse;
	}

	@RequestMapping(path = "/auth/1/session", method = RequestMethod.DELETE)
	public void login(@PathVariable(CONTEXT) Context context) {
		log.debug("[{}] logout", context);
	}

	@RequestMapping(path = "/api/2/myself", method = RequestMethod.GET)
	public JiraUser getMyself(@PathVariable(CONTEXT) Context context) {
		return new JiraUser("me", "myself");
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}", method = RequestMethod.GET)
	public JiraIssue getIssueByKey(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String key) {
		JiraIssue issue = getIssueMap(context).get(key);
		Assert.notNull(issue, "Issue " + key + " not found");
		return issue;
	}

	@RequestMapping(path = "/api/2/project/{projectKey}", method = RequestMethod.GET)
	public JiraProject getProjectByKey(@PathVariable(CONTEXT) Context context, @PathVariable("projectKey") String projectKey) {
		JiraProject jiraProject = getProjects(context).get(projectKey);
		Assert.notNull(jiraProject, "Project " + projectKey + " not found");
		return jiraProject;
	}

	@RequestMapping(path = "/api/2/priority", method = RequestMethod.GET)
	public List<JiraPriority> getPriorities(@PathVariable(CONTEXT) Context context) {
		return getData(context).getPriorities();
	}

	@RequestMapping(path = "/api/2/resolution", method = RequestMethod.GET)
	public List<JiraResolution> getResolutions(@PathVariable(CONTEXT) Context context) {
		return getData(context).getResolutions();
	}

	public List<JiraIssue> getAllIssues(Context context) {
		Map<String, JiraIssue> issuesPerKey = getIssueMap(context);
		return Collections.unmodifiableList(new ArrayList<>(issuesPerKey.values()));
	}

	private Map<String, JiraIssue> getIssueMap(Context context) {
		return getData(context).getIssues();
	}

	private JiraDummyData getData(Context context) {
		Assert.notNull(context);
		return data.computeIfAbsent(context, k -> new JiraDummyData());
	}

	@RequestMapping(path = "/api/2/issue/{issueId}/remotelink", method = RequestMethod.GET)
	public List<JiraRemoteLink> remoteLinks(@PathVariable(CONTEXT) Context context, @PathVariable("issueId") String issueId) {
		return getRemoteLinks(context, issueId);
	}

	public List<JiraRemoteLink> getRemoteLinks(Context context, JiraIssue issue) {
		return getRemoteLinks(context, issue.getKey());
	}

	private List<JiraRemoteLink> getRemoteLinks(Context context, String key) {
		Assert.notNull(key);
		Map<String, JiraRemoteLinks> remoteLinksPerIssueKey = getData(context).getRemoteLinks();
		JiraRemoteLinks jiraRemoteLinks = remoteLinksPerIssueKey.get(key);
		if (jiraRemoteLinks == null) {
			return Collections.emptyList();
		}
		return jiraRemoteLinks;
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/transitions", method = RequestMethod.GET)
	public JiraTransitions getTransitions(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey) {
		return new JiraTransitions(getData(context).getTransitions());
	}

	@RequestMapping(path = "/api/2/project/{projectKey}/versions", method = RequestMethod.GET)
	public List<JiraVersion> getVersions(@PathVariable(CONTEXT) Context context, @PathVariable("projectKey") String projectKey) {
		return getData(context).getVersions();
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/remotelink", method = RequestMethod.POST)
	public void addRemoteLink(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraRemoteLink newRemoteLink) {
		JiraRemoteLinks remoteLinks = getData(context).getRemoteLinks().computeIfAbsent(issueKey, k -> new JiraRemoteLinks());
		remoteLinks.add(newRemoteLink);
	}

	@RequestMapping(path = "/api/2/issue", method = RequestMethod.POST)
	public JiraIssue createIssue(@PathVariable(CONTEXT) Context context, @RequestBody JiraIssue issue) {
		JiraProject project = issue.getFields().getProject();
		validateProject(context, project);

		Assert.isNull(issue.getKey());
		Assert.isNull(issue.getId());
		if (issue.getKey() == null) {
			long id = getIssueMap(context).size() + 1;
			String projectKey = project.getKey();
			Assert.notNull(projectKey);
			issue.setKey(projectKey + "-" + id);
			issue.setId(String.valueOf(id));
		}
		if (issue.getFields().getStatus() == null) {
			JiraIssueStatus defaultStatus = getData(context).getDefaultStatus();
			Assert.notNull(defaultStatus, "defaultStatus must be set");
			issue.getFields().setStatus(defaultStatus);
		}

		if (issue.getFields().getResolution() != null) {
			validateResolution(context, issue.getFields().getResolution());
		}
		validateVersions(context, issue.getFields().getVersions());
		validateVersions(context, issue.getFields().getFixVersions());

		if (issue.getFields().getPriority() != null) {
			validatePriority(context, issue.getFields().getPriority());
		}

		Object old = getIssueMap(context).put(issue.getKey(), issue);
		Assert.isNull(old);
		return issue;
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}", method = RequestMethod.PUT)
	public void updateIssue(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueByKey(context, issueKey);
		Assert.isNull(jiraIssueUpdate.getTransition());
		updateFields(context, jiraIssueUpdate, issueInSystem);
	}

	private void validateValidVersion(Context context, JiraVersion version) {
		for (JiraVersion jiraVersion : getVersions(context, null)) {
			if (jiraVersion.getName().equals(version.getName()) && jiraVersion.getId().equals(version.getId())) {
				return;
			}
		}
		throw new IllegalArgumentException("Unknown version: " + version);
	}

	private void validatePriority(Context context, JiraPriority priority) {
		for (JiraPriority jiraPriority : getPriorities(context)) {
			if (jiraPriority.getName().equals(priority.getName()) && jiraPriority.getId().equals(priority.getId())) {
				return;
			}
		}
		throw new IllegalArgumentException("Unknown priority: " + priority);
	}


	private void validateProject(Context context, JiraProject project) {
		Assert.notNull(project);
		for (JiraProject jiraProject : getProjects(context).values()) {
			if (jiraProject.getKey().equals(project.getKey()) && jiraProject.getId().equals(project.getId())) {
				return;
			}
		}
		throw new IllegalArgumentException("Unknown project: " + project);
	}

	private void validateVersions(Context context, Set<JiraVersion> versions) {
		if (versions != null) {
			versions.forEach(version -> validateValidVersion(context, version));
		}
	}

	private void validateResolution(Context context, JiraResolution resolution) {
		for (JiraResolution jiraResolution : getResolutions(context)) {
			if (jiraResolution.getName().equals(resolution.getName()) && jiraResolution.getId().equals(resolution.getId())) {
				return;
			}
		}
		throw new IllegalArgumentException("Unknown resolution: " + resolution);
	}

	private void updateFields(Context context, JiraIssueUpdate jiraIssueUpdate, JiraIssue issueInSystem) {
		JiraFieldsUpdate fieldToUpdate = jiraIssueUpdate.getFields();
		if (fieldToUpdate == null) {
			return;
		}

		if (fieldToUpdate.getDescription() != null) {
			issueInSystem.getFields().setDescription(fieldToUpdate.getDescription());
		}

		if (fieldToUpdate.getResolution() != null) {
			validateResolution(context, fieldToUpdate.getResolution());
			issueInSystem.getFields().setResolution(fieldToUpdate.getResolution());
		}

		if (fieldToUpdate.getAssignee() != null) {
			issueInSystem.getFields().setAssignee(fieldToUpdate.getAssignee());
		}

		validateVersions(context, fieldToUpdate.getFixVersions());

		if (fieldToUpdate.getFixVersions() != null) {
			issueInSystem.getFields().setFixVersions(fieldToUpdate.getFixVersions());
		}

		Assert.isNull(fieldToUpdate.getLabels());
		Assert.isNull(fieldToUpdate.getVersions());
		Assert.isNull(fieldToUpdate.getPriority());
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/transitions", method = RequestMethod.POST)
	public void transitionIssue(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueByKey(context, issueKey);
		Assert.notNull(jiraIssueUpdate.getTransition());

		JiraIssueStatus targetStatus = jiraIssueUpdate.getTransition().getTo();
		Assert.notNull(targetStatus);
		log.debug("Updating status of {} to {}", issueKey, targetStatus);
		issueInSystem.getFields().setStatus(targetStatus);

		updateFields(context, jiraIssueUpdate, issueInSystem);
	}
}
