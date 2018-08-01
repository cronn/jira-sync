package de.cronn.jira.sync.dummy;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraComments;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraFieldsUpdate;
import de.cronn.jira.sync.domain.JiraFilterResult;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueHistoryItem;
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
import de.cronn.jira.sync.domain.WellKnownJiraField;

@RestController
@RequestMapping("/{" + JiraDummyService.CONTEXT + "}/rest")
public class JiraDummyService {

	private static final Logger log = LoggerFactory.getLogger(JiraDummyService.class);

	public static final String CONTEXT = "context";
	public static final String AUTHORIZATION = "Authorization";

	private final Map<Context, JiraDummyData> data = new EnumMap<>(Context.class);

	private Clock clock;

	@Autowired
	public void setClock(Clock clock) {
		this.clock = clock;
	}

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

	public void expectBasicAuth(Context context, String username, String password) {
		getData(context).setBasicAuthCredentials(username, password);
	}

	public void setDefaultStatus(Context context, JiraIssueStatus status) {
		getData(context).setDefaultStatus(status);
	}

	public void addProject(Context context, JiraProject project) {
		Assert.notNull(project.getKey(), "project.key must not be null");
		Assert.notNull(project.getId(), "project.id must not be null");
		Object old = getProjects(context).putIfAbsent(project.getKey(), project);
		Assert.isNull(old, "project " + project.getKey() + " already exists");
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

	public void setDefaultPriority(Context context, JiraPriority priority) {
		getData(context).setDefaultPriority(priority);
	}

	public void addUser(Context context, JiraUser user) {
		getData(context).addUser(user);
	}

	public void addField(Context context, JiraField field, Map<String, Long> allowedValues) {
		getData(context).addField(field, allowedValues);
	}

	public void addResolution(Context context, JiraResolution resolution) {
		getData(context).getResolutions().add(resolution);
	}

	public void associateFilterIdToProject(Context context, String filterId, JiraProject project) {
		validateProject(context, project);
		Map<String, JiraProject> projectAssociatedToFilterId = getData(context).getProjectAssociatedToFilterId();
		Object old = projectAssociatedToFilterId.putIfAbsent(filterId, project);
		Assert.isNull(old, "project already associated to filter " + filterId);
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
		result.setJql(filterId);
		return result;
	}

	@RequestMapping(path = "/api/2/search", method = RequestMethod.GET)
	public JiraSearchResult search(@PathVariable(CONTEXT) Context context, @RequestParam("jql") String jql) {
		JiraSearchResult result = new JiraSearchResult();

		Map<String, JiraProject> projectAssociatedToFilterId = getData(context).getProjectAssociatedToFilterId();
		JiraProject project = projectAssociatedToFilterId.get(jql);
		Assert.notNull(project, "No project associated to filter " + jql);

		List<JiraIssue> allIssues = getAllIssues(context).stream()
			.filter(issue -> issue.getFields().getProject().getKey().equals(project.getKey()))
			.collect(Collectors.toList());

		result.setIssues(allIssues);
		result.setMaxResults(allIssues.size());
		result.setTotal(allIssues.size());
		return result;
	}

	@RequestMapping(path = "/auth/1/session", method = RequestMethod.POST)
	public JiraLoginResponse login(@PathVariable(CONTEXT) Context context, @RequestBody JiraLoginRequest loginRequest,
								   @RequestHeader(value = AUTHORIZATION, required = false) String authorization) {
		validateLoginCredentials(context, loginRequest);
		validateBasicAuthCredentials(context, authorization);
		JiraLoginResponse loginResponse = new JiraLoginResponse();
		JiraSession session = new JiraSession();
		session.setName("test-session-" + context);
		session.setValue("test-session-" + context);
		loginResponse.setSession(session);

		log.debug("[{}] login", context);

		return loginResponse;
	}

	private void validateLoginCredentials(@PathVariable(CONTEXT) Context context, @RequestBody JiraLoginRequest loginRequest) {
		JiraLoginRequest credentials = getData(context).getCredentials();
		if (!credentials.getUsername().equals(loginRequest.getUsername())) {
			throw new IllegalArgumentException("Illegal username");
		}
		if (!credentials.getPassword().equals(loginRequest.getPassword())) {
			throw new IllegalArgumentException("Illegal password");
		}
	}

	private void validateBasicAuthCredentials(@PathVariable(CONTEXT) Context context, String authorization) {
		BasicAuthCredentials credentials = getData(context).getBasicAuthCredentials();
		if (credentials != null) {
			String encoded = credentials.encodeBase64();
			if (!encoded.equals(authorization)) {
				throw new IllegalArgumentException("Illegal basic auth credentials");
			}
		}
	}

	@RequestMapping(path = "/auth/1/session", method = RequestMethod.DELETE)
	public void logout(@PathVariable(CONTEXT) Context context) {
		log.debug("[{}] logout", context);
	}

	@RequestMapping(path = "/api/2/myself", method = RequestMethod.GET)
	public JiraUser getMyself(@PathVariable(CONTEXT) Context context) {
		return new JiraUser("me", "myself", "my self");
	}

	@RequestMapping(path = "/api/2/user", method = RequestMethod.GET)
	public ResponseEntity<Object> getUser(@PathVariable(CONTEXT) Context context, @RequestParam("username") String username) {
		Assert.hasText(username, "username must not be empty");
		JiraUser user = getData(context).getUser(username);
		if (user == null) {
			return new ResponseEntity<>("user not found", HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(user, HttpStatus.OK);
		}
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}", method = RequestMethod.GET)
	public JiraIssue getIssueByKey(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String key) {
		return getIssueByKey(context, key, null);
	}

	@RequestMapping(value = "/api/2/issue/{issueKey}", params = "expand", method = RequestMethod.GET)
	public JiraIssue getIssueByKey(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String key, @RequestParam(name = "expand") String[] expandParams) {
		JiraIssue issue = getIssueMap(context).get(key);
		Assert.notNull(issue, "Issue " + key + " not found");

		JiraIssue result = SerializationUtils.clone(issue);

		if (!ArrayUtils.contains(expandParams, "changelog")){
			result.setChangelog(null);
		}

		return result;
	}

	@RequestMapping(path = "/api/2/issue/createmeta", method = RequestMethod.GET)
	public Map<String, Object> getIssueCreateMeta(@PathVariable(CONTEXT) Context context) {
		List<Map<String, Object>> projectsMeta = new ArrayList<>();
		JiraDummyData data = getData(context);
		for (JiraProject project : data.getProjects().values()) {
			Map<String, Object> projectMeta = new LinkedHashMap<>();
			projectMeta.put("key", project.getKey());
			projectMeta.put("name", project.getName());
			Map<String, Object> fieldsMeta = new LinkedHashMap<>();
			for (Entry<JiraField, Map<String, Long>> entry : data.getCustomFields().entrySet()) {
				Map<String, Object> customFieldMeta = new LinkedHashMap<>();
				JiraField customField = entry.getKey();
				customFieldMeta.put("name", customField.getName());
				List<Map<String, Object>> allowedValues = new ArrayList<>();
				Map<String, Long> customFieldAllowedValues = entry.getValue();
				if (customFieldAllowedValues != null) {
					for (Entry<String, Long> allowedValue : customFieldAllowedValues.entrySet()) {
						Map<String, Object> allowedValueMap = new LinkedHashMap<>();
						allowedValueMap.put("value", allowedValue.getKey());
						allowedValueMap.put("id", allowedValue.getValue());
						allowedValues.add(allowedValueMap);
					}
					customFieldMeta.put("allowedValues", allowedValues);
				}
				fieldsMeta.put(customField.getId(), customFieldMeta);
			}
			List<Map<String, Map<String, Object>>> issueTypes = new ArrayList<Map<String, Map<String, Object>>>();
			Map<String, Object> unknownIssueType = new LinkedHashMap<>();
			unknownIssueType.put("name", "unknown issueType");
			issueTypes.add(Collections.singletonMap("fields", null));
			issueTypes.add(Collections.singletonMap("fields", fieldsMeta));
			projectMeta.put("issuetypes", issueTypes);
			projectsMeta.add(projectMeta);
		}
		return Collections.singletonMap("projects", projectsMeta);
	}

	@RequestMapping(path = "/api/2/project/{projectKey}", method = RequestMethod.GET)
	public JiraProject getProjectByKey(@PathVariable(CONTEXT) Context context, @PathVariable("projectKey") String projectKey) {
		JiraProject jiraProject = getProjects(context).get(projectKey);
		Assert.notNull(jiraProject, "Project " + projectKey + " not found");
		return jiraProject;
	}

	@RequestMapping(path = "/api/2/project", method = RequestMethod.GET)
	public List<JiraProject> getAllProjects(@PathVariable(CONTEXT) Context context) {
		return new ArrayList<>(getProjects(context).values());
	}

	@RequestMapping(path = "/api/2/priority", method = RequestMethod.GET)
	public List<JiraPriority> getPriorities(@PathVariable(CONTEXT) Context context) {
		return getData(context).getPriorities();
	}

	@RequestMapping(path = "/api/2/field", method = RequestMethod.GET)
	public List<JiraField> getFields(@PathVariable(CONTEXT) Context context) {
		Map<JiraField, Map<String, Long>> customFields = getData(context).getCustomFields();
		return new ArrayList<>(customFields.keySet());
	}

	@RequestMapping(path = "/api/2/resolution", method = RequestMethod.GET)
	public List<JiraResolution> getResolutions(@PathVariable(CONTEXT) Context context) {
		return getData(context).getResolutions();
	}

	public Set<JiraIssue> getAllIssues(Context context) {
		Map<String, JiraIssue> issuesPerKey = getIssueMap(context);
		return Collections.unmodifiableSet(new LinkedHashSet<>(issuesPerKey.values()));
	}

	private Map<String, JiraIssue> getIssueMap(Context context) {
		return getData(context).getIssues();
	}

	private JiraDummyData getData(Context context) {
		Assert.notNull(context, "context must not be null");
		return data.computeIfAbsent(context, k -> new JiraDummyData());
	}

	@RequestMapping(path = "/api/2/issue/{issueId}/remotelink", method = RequestMethod.GET)
	public List<JiraRemoteLink> remoteLinks(@PathVariable(CONTEXT) Context context, @PathVariable("issueId") String issueId) {
		return getRemoteLinks(context, issueId);
	}

	public List<JiraRemoteLink> getRemoteLinks(Context context, JiraIssue issue) {
		return getRemoteLinks(context, issue.getKey());
	}

	private List<JiraRemoteLink> getRemoteLinks(Context context, String issueKey) {
		Assert.notNull(issueKey, "issueKey must be set");
		JiraIssue issue = getIssueMap(context).get(issueKey);
		Map<String, JiraRemoteLinks> remoteLinksPerIssueId = getData(context).getRemoteLinks();
		JiraRemoteLinks jiraRemoteLinks = remoteLinksPerIssueId.get(issue.getId());
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
		JiraIssue issue = getIssueMap(context).get(issueKey);
		JiraRemoteLinks remoteLinks = getData(context).getRemoteLinks().computeIfAbsent(issue.getId(), k -> new JiraRemoteLinks());
		remoteLinks.add(newRemoteLink);
		refreshUpdatedTimestamp(issue);
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/comment", method = RequestMethod.POST)
	public ResponseEntity<Object> addComment(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraComment comment) {
		String body = comment.getBody();
		if (body == null) {
			return new ResponseEntity<>("body must not be empty", HttpStatus.BAD_REQUEST);
		}

		JiraIssue issue = getIssueMap(context).get(issueKey);
		JiraIssueFields fields = issue.getOrCreateFields();
		JiraComments comments = fields.getOrCreateComment();

		ZonedDateTime now = ZonedDateTime.now(clock);
		comment.setCreated(now);
		comment.setUpdated(now);
		comment.setId(generateCommentId(issue, comments));
		comment.setAuthor(getMyself(context));

		comments.addComment(comment);

		return new ResponseEntity<>(comment, HttpStatus.OK);
	}

	private static String generateCommentId(JiraIssue issue, JiraComments comments) {
		return issue.getId() + "_" + (comments.getComments().size() + 1);
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/comment/{commentId}", method = RequestMethod.PUT)
	public ResponseEntity<Object> updateComment(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @PathVariable("commentId") String commentId, @RequestBody JiraComment comment) {
		String body = comment.getBody();
		if (body == null) {
			return new ResponseEntity<>("body must not be empty", HttpStatus.BAD_REQUEST);
		}

		JiraIssue issue = getIssueMap(context).get(issueKey);
		JiraIssueFields fields = issue.getOrCreateFields();
		JiraComments comments = fields.getOrCreateComment();

		JiraComment commentToUpdate = comments.getComments().stream()
			.filter(c -> c.getId().equals(commentId))
			.findFirst()
			.orElse(null);

		if (commentToUpdate == null) {
			return new ResponseEntity<>("comment " + commentId + " not found", HttpStatus.NOT_FOUND);
		}

		ZonedDateTime now = ZonedDateTime.now(clock);
		commentToUpdate.setUpdated(now);
		commentToUpdate.setBody(comment.getBody());

		refreshUpdatedTimestamp(issue);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(path = "/api/2/issue", method = RequestMethod.POST)
	public ResponseEntity<Object> createIssue(@PathVariable(CONTEXT) Context context, @RequestBody JiraIssue issue) {
		JiraIssueFields fields = issue.getFields();

		if (fields == null) {
			return new ResponseEntity<>("fields are missing", HttpStatus.BAD_REQUEST);
		}

		JiraProject project = fields.getProject();

		try {
			validateProject(context, project);
		} catch (Exception e) {
			log.error("createIssue failed", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

		Assert.isNull(issue.getKey(), "issue.key aready set");
		Assert.isNull(issue.getId(), "issue.id already set");
		issue.setKey(generateKey(context, project));
		issue.setId(generateId(context));
		if (fields.getStatus() == null) {
			JiraIssueStatus defaultStatus = getData(context).getDefaultStatus();
			Assert.notNull(defaultStatus, "defaultStatus must be set");
			fields.setStatus(defaultStatus);
		}

		if (fields.getResolution() != null) {
			validateResolution(context, fields.getResolution());
		}
		validateVersions(context, fields.getVersions());
		validateVersions(context, fields.getFixVersions());

		if (fields.getPriority() == null) {
			JiraPriority defaultPriority = getData(context).getDefaultPriority();
			Assert.notNull(defaultPriority, "defaultPriority must be set");
			fields.setPriority(defaultPriority);
		}

		validatePriority(context, fields.getPriority());

		refreshUpdatedTimestamp(issue);

		registerIssue(context, issue);
		return new ResponseEntity<>(issue, HttpStatus.OK);
	}

	private void registerIssue(Context context, JiraIssue issue) {
		String issueKey = issue.getKey();
		Assert.hasText(issueKey, "issueKey must not be empty");
		Object old = getIssueMap(context).put(issueKey, issue);
		Assert.isNull(old, "an issue with key '" + issueKey + "' is already registered: " + old);
	}

	private String generateId(@PathVariable(CONTEXT) Context context) {
		AtomicLong idCounter = getData(context).getIdCounter();
		return String.valueOf(idCounter.incrementAndGet());
	}

	private String generateKey(@PathVariable(CONTEXT) Context context, JiraProject project) {
		String projectKey = project.getKey();
		Assert.notNull(projectKey, "projectKey must not be null");
		AtomicLong keyCounter = getData(context).getOrCreateKeyCounter(projectKey);
		long id = keyCounter.incrementAndGet();
		return projectKey + "-" + id;
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}", method = RequestMethod.PUT)
	public void updateIssue(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueMap(context).get(issueKey);
		Assert.isNull(jiraIssueUpdate.getTransition(), "jiraIssueUpdate.transition must not be null");

		JiraIssueHistoryEntry historyEntry = createJiraIssueHistoryEntry();
		issueInSystem.getOrCreateChangeLog().addHistoryEntry(historyEntry);

		updateFields(context, jiraIssueUpdate, issueInSystem, historyEntry);
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
		Assert.notNull(project, "project must not be null");
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

	private void updateFields(Context context, JiraIssueUpdate jiraIssueUpdate, JiraIssue issueInSystem, JiraIssueHistoryEntry historyEntry) {
		JiraFieldsUpdate fieldToUpdate = jiraIssueUpdate.getFields();
		if (fieldToUpdate == null) {
			return;
		}

		if (fieldToUpdate.getDescription() != null) {
			historyEntry.addItem(new JiraIssueHistoryItem(WellKnownJiraField.DESCRIPTION)
				.withFromString(issueInSystem.getFields().getDescription())
				.withToString(fieldToUpdate.getDescription()));
			issueInSystem.getFields().setDescription(fieldToUpdate.getDescription());
		}

		if (fieldToUpdate.getResolution() != null) {
			validateResolution(context, fieldToUpdate.getResolution());
			historyEntry.addItem(new JiraIssueHistoryItem(WellKnownJiraField.RESOLUTION)
				.withFromString(getResolutionNameOrNull(issueInSystem))
				.withToString(fieldToUpdate.getResolution().getName()));
			issueInSystem.getFields().setResolution(fieldToUpdate.getResolution());
		}

		if (fieldToUpdate.getAssignee() != null) {
			historyEntry.addItem(new JiraIssueHistoryItem(WellKnownJiraField.ASSIGNEE)
				.withFromString(getAssigneeNameOrNull(issueInSystem))
				.withToString(fieldToUpdate.getAssignee().getName()));
			issueInSystem.getFields().setAssignee(fieldToUpdate.getAssignee());
		}

		validateVersions(context, fieldToUpdate.getFixVersions());
		validateVersions(context, fieldToUpdate.getVersions());

		if (fieldToUpdate.getFixVersions() != null) {
			issueInSystem.getFields().setFixVersions(fieldToUpdate.getFixVersions());
		}

		if (fieldToUpdate.getVersions() != null) {
			issueInSystem.getFields().setVersions(fieldToUpdate.getVersions());
		}

		for (Entry<String, Object> entry : fieldToUpdate.getOther().entrySet()) {
			issueInSystem.getFields().setOther(entry.getKey(), entry.getValue());
		}

		Assert.isNull(fieldToUpdate.getLabels(), "labels must be null");
		Assert.isNull(fieldToUpdate.getPriority(), "priority must be null");

		refreshUpdatedTimestamp(issueInSystem);
	}

	private String getAssigneeNameOrNull(JiraIssue issue) {
		return issue.getFields().getAssignee() != null ? issue.getFields().getAssignee().getName() : null;
	}

	private String getResolutionNameOrNull(JiraIssue issue) {
		return issue.getFields().getResolution() != null ? issue.getFields().getResolution().getName() : null;
	}

	private void refreshUpdatedTimestamp(JiraIssue issue) {
		issue.getFields().setUpdated(ZonedDateTime.now(clock));
	}

	@RequestMapping(path = "/api/2/issue/{issueKey}/transitions", method = RequestMethod.POST)
	public void transitionIssue(@PathVariable(CONTEXT) Context context, @PathVariable("issueKey") String issueKey, @RequestBody JiraIssueUpdate jiraIssueUpdate) {
		JiraIssue issueInSystem = getIssueMap(context).get(issueKey);
		Assert.notNull(jiraIssueUpdate.getTransition(), "transition must not be null");

		JiraIssueStatus targetStatus = jiraIssueUpdate.getTransition().getTo();
		Assert.notNull(targetStatus, "targetStatus must not be null");
		log.debug("Updating status of {} to {}", issueKey, targetStatus);
		JiraIssueHistoryEntry historyEntry = createHistoryEntryForTransition(issueInSystem.getFields().getStatus(), targetStatus);
		issueInSystem.getOrCreateChangeLog().addHistoryEntry(historyEntry);

		issueInSystem.getFields().setStatus(targetStatus);
		updateFields(context, jiraIssueUpdate, issueInSystem, historyEntry);
	}

	private JiraIssueHistoryEntry createHistoryEntryForTransition(JiraIssueStatus from, JiraIssueStatus to) {
		JiraIssueHistoryEntry historyEntry = createJiraIssueHistoryEntry();
		historyEntry.addItem(JiraIssueHistoryItem.createStatusTransition(from.getName(), to.getName()));

		return historyEntry;
	}

	private JiraIssueHistoryEntry createJiraIssueHistoryEntry() {
		JiraIssueHistoryEntry historyEntry = new JiraIssueHistoryEntry().withCreated(ZonedDateTime.now(clock));
		return historyEntry;
	}

	public void moveIssue(Context context, String issueKey, String projectKey) {
		JiraIssue issue = getIssueMap(context).get(issueKey);
		JiraProject project = getProjectByKey(context, projectKey);
		issue.getFields().setProject(project);
		issue.setKey(generateKey(context, project));

		registerIssue(context, issue);
		refreshUpdatedTimestamp(issue);
	}

}
