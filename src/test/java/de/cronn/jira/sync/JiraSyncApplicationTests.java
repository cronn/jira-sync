package de.cronn.jira.sync;

import static de.cronn.jira.sync.dummy.JiraDummyService.Context.*;
import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraFieldSchema;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraRemoteLinkObject;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.dummy.JiraDummyService;
import de.cronn.jira.sync.dummy.JiraDummyService.Context;
import de.cronn.jira.sync.service.JiraService;
import de.cronn.jira.sync.strategy.SyncResult;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("/test.properties")
public class JiraSyncApplicationTests {

	private static final JiraProject SOURCE_PROJECT = new JiraProject("1", "PROJECT_ONE");
	private static final JiraProject SOURCE_PROJECT_OTHER = new JiraProject("2", "PROJECT_OTHER");
	private static final JiraProject TARGET_PROJECT = new JiraProject("100", "PRJ_ONE");

	private static final JiraIssueStatus SOURCE_STATUS_OPEN = new JiraIssueStatus("1", "Open");
	private static final JiraIssueStatus SOURCE_STATUS_REOPENED = new JiraIssueStatus("2", "Reopened");
	private static final JiraIssueStatus SOURCE_STATUS_IN_PROGRESS = new JiraIssueStatus("3", "In Progress");
	private static final JiraIssueStatus SOURCE_STATUS_RESOLVED = new JiraIssueStatus("4", "Resolved");
	private static final JiraIssueStatus SOURCE_STATUS_CLOSED = new JiraIssueStatus("5", "Closed");
	private static final JiraIssueStatus TARGET_STATUS_OPEN = new JiraIssueStatus("100", "Open");
	private static final JiraIssueStatus TARGET_STATUS_REOPENED = new JiraIssueStatus("101", "Reopened");
	private static final JiraIssueStatus TARGET_STATUS_RESOLVED = new JiraIssueStatus("102", "Resolved");
	private static final JiraIssueStatus TARGET_STATUS_CLOSED = new JiraIssueStatus("103", "Closed");

	private static final JiraIssueType SOURCE_TYPE_BUG = new JiraIssueType("1", "Bug");
	private static final JiraIssueType SOURCE_TYPE_UNKNOWN = new JiraIssueType("2", "Unknown");

	private static final JiraIssueType TARGET_TYPE_BUG = new JiraIssueType("100", "Bug");
	private static final JiraIssueType TARGET_TYPE_IMPROVEMENT = new JiraIssueType("101", "Improvement");
	private static final JiraIssueType TARGET_TYPE_TASK = new JiraIssueType("102", "Task");

	private static final JiraPriority SOURCE_PRIORITY_HIGH = new JiraPriority("1", "High");
	private static final JiraPriority SOURCE_PRIORITY_UNMAPPED = new JiraPriority("99", "Unmapped priority");

	private static final JiraPriority TARGET_PRIORITY_DEFAULT = new JiraPriority("100", "Default");
	private static final JiraPriority TARGET_PRIORITY_CRITICAL = new JiraPriority("101", "Critical");

	private static final JiraResolution SOURCE_RESOLUTION_FIXED = new JiraResolution("1", "Fixed");
	private static final JiraResolution TARGET_RESOLUTION_DONE = new JiraResolution("100", "Done");

	private static final JiraVersion SOURCE_VERSION_10 = new JiraVersion("1", "10.0");
	private static final JiraVersion SOURCE_VERSION_11 = new JiraVersion("2", "11.0");
	private static final JiraVersion SOURCE_VERSION_UNDEFINED = new JiraVersion("98", "Undefined");
	private static final JiraVersion SOURCE_VERSION_UNMAPPED = new JiraVersion("99", "Unmapped version");

	private static final JiraVersion TARGET_VERSION_10 = new JiraVersion("100", "10");
	private static final JiraVersion TARGET_VERSION_11 = new JiraVersion("101", "11");

	private static final JiraUser SOURCE_USER_SOME = new JiraUser("some.user", "some.user", "Some User");
	private static final JiraUser SOURCE_USER_ANOTHER = new JiraUser("anotheruser", "anotheruser", "Another User");

	private static final JiraFieldSchema FIELD_SCHEMA_LABELS = new JiraFieldSchema(null, null, "com.atlassian.jira.plugin.system.customfieldtypes:labels");
	private static final JiraFieldSchema FIELD_SCHEMA_SELECT = new JiraFieldSchema(null, null, "com.atlassian.jira.plugin.system.customfieldtypes:select");

	private static final JiraField SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION = new JiraField("1", "Found in version", true, FIELD_SCHEMA_LABELS);
	private static final JiraField TARGET_CUSTOM_FIELD_FOUND_IN_VERSION = new JiraField("100", "Found in software version", true, FIELD_SCHEMA_LABELS);

	private static final JiraField SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION = new JiraField("2", "Fixed in version", true, FIELD_SCHEMA_SELECT);
	private static final JiraField TARGET_CUSTOM_FIELD_FIXED_IN_VERSION = new JiraField("200", "Fixed in software version", true, FIELD_SCHEMA_SELECT);

	@Autowired
	private TestClock clock;

	@Autowired
	private JiraDummyService jiraDummyService;

	@Autowired
	private JiraService jiraSource;

	@Autowired
	private JiraService jiraTarget;

	@Autowired
	private JiraSyncTask syncTask;

	@Autowired
	private JiraSyncConfig syncConfig;

	@LocalServerPort
	private int port;

	private String sourceBaseUrl;

	private String targetBaseUrl;

	@Before
	public void resetClock() {
		clock.reset();
	}

	@Before
	public void setUp() throws Exception {

		String commonBaseUrl = "https://localhost:" + port + "/";
		sourceBaseUrl = commonBaseUrl + Context.SOURCE + "/";
		targetBaseUrl = commonBaseUrl + Context.TARGET + "/";

		syncConfig.getSource().setUrl(new URL(sourceBaseUrl));
		syncConfig.getTarget().setUrl(new URL(targetBaseUrl));

		jiraDummyService.reset();

		jiraSource.evictAllCaches();
		jiraTarget.evictAllCaches();

		jiraDummyService.setBaseUrl(SOURCE, sourceBaseUrl);
		jiraDummyService.setBaseUrl(TARGET, targetBaseUrl);

		jiraDummyService.addProject(SOURCE, SOURCE_PROJECT);
		jiraDummyService.addProject(SOURCE, SOURCE_PROJECT_OTHER);
		jiraDummyService.addProject(TARGET, TARGET_PROJECT);

		jiraDummyService.associateFilterIdToProject(SOURCE, "12345", SOURCE_PROJECT);

		jiraDummyService.addPriority(SOURCE, SOURCE_PRIORITY_HIGH);
		jiraDummyService.addPriority(SOURCE, SOURCE_PRIORITY_UNMAPPED);
		jiraDummyService.addPriority(TARGET, TARGET_PRIORITY_DEFAULT);
		jiraDummyService.addPriority(TARGET, TARGET_PRIORITY_CRITICAL);

		jiraDummyService.setDefaultPriority(TARGET, TARGET_PRIORITY_DEFAULT);

		jiraDummyService.addResolution(SOURCE, SOURCE_RESOLUTION_FIXED);
		jiraDummyService.addResolution(TARGET, TARGET_RESOLUTION_DONE);

		jiraDummyService.addTransition(SOURCE, new JiraTransition("1", "Set resolved", SOURCE_STATUS_RESOLVED));
		jiraDummyService.addTransition(SOURCE, new JiraTransition("2", "Set in progress", SOURCE_STATUS_IN_PROGRESS));
		jiraDummyService.addTransition(SOURCE, new JiraTransition("3", "Close", SOURCE_STATUS_CLOSED));
		jiraDummyService.addTransition(SOURCE, new JiraTransition("4", "Reopen", SOURCE_STATUS_REOPENED));

		jiraDummyService.addTransition(TARGET, new JiraTransition("100", "Resolve", TARGET_STATUS_RESOLVED));
		jiraDummyService.addTransition(TARGET, new JiraTransition("101", "Close", TARGET_STATUS_CLOSED));
		jiraDummyService.addTransition(TARGET, new JiraTransition("102", "Reopen", TARGET_STATUS_REOPENED));

		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_10);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_11);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_UNDEFINED);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_UNMAPPED);

		jiraDummyService.addVersion(TARGET, TARGET_VERSION_10);
		jiraDummyService.addVersion(TARGET, TARGET_VERSION_11);

		jiraDummyService.addUser(SOURCE, SOURCE_USER_SOME);
		jiraDummyService.addUser(SOURCE, SOURCE_USER_ANOTHER);

		jiraDummyService.addField(SOURCE, SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION, null);
		jiraDummyService.addField(TARGET, TARGET_CUSTOM_FIELD_FOUND_IN_VERSION, null);

		jiraDummyService.addField(SOURCE, SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION, Collections.singletonMap("v1", 10L));
		jiraDummyService.addField(TARGET, TARGET_CUSTOM_FIELD_FIXED_IN_VERSION, Collections.singletonMap("1.0", 100L));

		jiraDummyService.setDefaultStatus(TARGET, TARGET_STATUS_OPEN);

		TARGET_PROJECT.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));

		jiraDummyService.expectLoginRequest(SOURCE, "jira-sync", "secret in source");
		jiraDummyService.expectLoginRequest(TARGET, "jira-sync", "secret in target");

		jiraDummyService.expectBasicAuth(TARGET, "basic-auth-user", "secret");

		jiraSource.login(syncConfig.getSource());
		jiraTarget.login(syncConfig.getTarget());
	}

	@After
	public void logOut() {
		jiraSource.logout();
		jiraTarget.logout();
	}

	private static Map<String, Object> idValueMap(int id, String value) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", id);
		map.put("value", value);
		return map;
	}

	@Test
	public void testConfiguration() throws Exception {
		Map<String, String> resolutionMapping = syncConfig.getResolutionMapping();

		assertThat(resolutionMapping).containsExactly(
			entry("Done", "Fixed"),
			entry("Won't Fix", "Won't Fix"),
			entry("Won't Do", "Rejected"),
			entry("Incomplete", "Incomplete"),
			entry("Fixed", "Fixed"),
			entry("Cannot Reproduce", "Cannot Reproduce"),
			entry("Duplicate", "Duplicate")
		);
	}

	@Test
	public void testResolutionsAreCached() throws Exception {
		assertThat(jiraSource).isNotSameAs(jiraTarget);

		jiraSource.getResolutions();
		List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
		List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();
		assertThat(sourceResolutions1).isSameAs(sourceResolutions2);

		jiraTarget.getResolutions();
		List<JiraResolution> targetResolutions1 = jiraTarget.getResolutions();
		List<JiraResolution> targetResolutions2 = jiraTarget.getResolutions();
		assertThat(targetResolutions1).isSameAs(targetResolutions2);

		assertThat(sourceResolutions1).isNotSameAs(targetResolutions1);
	}

	@Test
	public void testResolutionsAndPrioritiesAreCached() throws Exception {
		jiraSource.getResolutions();
		List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
		List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();

		jiraSource.getPriorities();
		List<JiraPriority> sourcePriorities1 = jiraSource.getPriorities();
		List<JiraPriority> sourcePriorities2 = jiraSource.getPriorities();
		assertThat(sourceResolutions1).isSameAs(sourceResolutions2);
		assertThat(sourcePriorities1).isSameAs(sourcePriorities2);
		assertThat(sourcePriorities1).isNotSameAs(sourceResolutions1);
	}

	@Test
	public void testEmptySync() throws Exception {
		// when
		syncAndAssertNoChanges();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET)).isEmpty();
	}

	@Test
	public void testCreateTicketInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("label1", "label2")));
		sourceIssue.getFields().setVersions(new LinkedHashSet<>(Arrays.asList(SOURCE_VERSION_10, SOURCE_VERSION_11, SOURCE_VERSION_UNDEFINED)));
		sourceIssue.getFields().setFixVersions(Collections.singleton(SOURCE_VERSION_11));
		JiraIssue createdSourceIssue = jiraSource.createIssue(sourceIssue);

		clock.windForwardSeconds(30);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		JiraIssueFields targetIssueFields = targetIssue.getFields();
		assertThat(targetIssueFields.getSummary()).isEqualTo("PROJECT_ONE-1: My first bug");
		assertThat(targetIssueFields.getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssueFields.getPriority().getName()).isEqualTo(TARGET_PRIORITY_CRITICAL.getName());
		assertThat(targetIssueFields.getLabels()).containsExactly("label1", "label2");
		assertThat(getNames(targetIssueFields.getVersions())).containsExactlyInAnyOrder("10", "11");
		assertThat(getNames(targetIssueFields.getFixVersions())).containsExactly("11");
		assertThat(targetIssueFields.getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		List<JiraRemoteLink> remoteLinksInTarget = jiraDummyService.getRemoteLinks(TARGET, targetIssue);
		List<JiraRemoteLink> remoteLinksInSource = jiraDummyService.getRemoteLinks(SOURCE, createdSourceIssue);
		assertThat(remoteLinksInTarget).hasSize(1);
		assertThat(remoteLinksInSource).hasSize(1);

		JiraRemoteLinkObject firstRemoteLinkInSource = remoteLinksInSource.iterator().next().getObject();
		assertThat(firstRemoteLinkInSource.getUrl()).isEqualTo(new URL(targetBaseUrl + "/browse/PRJ_ONE-1"));
		assertThat(firstRemoteLinkInSource.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-source/favicon.ico"));

		JiraRemoteLinkObject firstRemoteLinkInTarget = remoteLinksInTarget.iterator().next().getObject();
		assertThat(firstRemoteLinkInTarget.getUrl()).isEqualTo(new URL(sourceBaseUrl + "/browse/PROJECT_ONE-1"));
		assertThat(firstRemoteLinkInTarget.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-target/favicon.ico"));

		syncAndAssertNoChanges();
	}

	@Test
	public void testUnmappedVersionAndUnmappedPriority() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_UNMAPPED);
		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_UNMAPPED));
		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssue.getFields().getPriority().getName()).isEqualTo(TARGET_PRIORITY_DEFAULT.getName());
		assertThat(targetIssue.getFields().getVersions()).isEmpty();

		syncAndAssertNoChanges();
	}

	private JiraIssue getSingleIssue(Context context) {
		Set<JiraIssue> issues = jiraDummyService.getAllIssues(context);
		assertThat(issues).hasSize(1);
		return issues.iterator().next();
	}

	private static List<String> getNames(Set<JiraVersion> versions) {
		if (versions == null) {
			return null;
		}
		return versions.stream()
			.map(JiraVersion::getName)
			.collect(Collectors.toList());
	}

	@Test
	public void testErrorHandling() throws Exception {
		try {
			jiraSource.createIssue(new JiraIssue());
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e).hasMessage("[https://localhost:" + port + "/SOURCE/] Bad Request: fields are missing");
		}
	}

	@Test
	public void testCreateTicketInTarget_WithFallbackType() throws Exception {
		// given
		createIssueInSource(SOURCE_TYPE_UNKNOWN);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_TASK.getName());

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicketInTarget_WithCustomField() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1"));

		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_TASK.getName());
		assertThat(targetIssue.getFields().getOther()).containsExactly(entry(TARGET_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1")));

		syncAndAssertNoChanges();
	}

	@Test
	public void testUpdateTicketInTarget() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("");

		// when
		JiraIssueUpdate update = new JiraIssueUpdate();
		update.getOrCreateFields().setDescription("changed description");
		jiraSource.updateIssue(createdSourceIssue.getKey(), update);

		Instant beforeSecondUpdate = Instant.now(clock);
		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged description\n{panel}");
		assertThat(targetIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		JiraIssue sourceIssue = getSingleIssue(SOURCE);
		assertThat(sourceIssue.getFields().getUpdated().toInstant()).isEqualTo(beforeSecondUpdate);

		syncAndAssertNoChanges();
	}

	@Test
	public void testSetTicketToResolvedInSourceWhenTargetTicketIsClosed() throws Exception {
		// given
		createIssueInSource();

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);

		// when
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_RESOLVED.getName());
		assertThat(updatedSourceIssue.getFields().getResolution().getName()).isEqualTo(SOURCE_RESOLUTION_FIXED.getName());
		assertThat(getNames(updatedSourceIssue.getFields().getFixVersions())).containsExactly(SOURCE_VERSION_10.getName());

		syncAndAssertNoChanges();
	}

	@Test
	public void testCopyCustomFieldsWhenIssueIsClosed() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);

		jiraSource.createIssue(sourceIssue);

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);

		// when
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		update.getOrCreateFields().setOther(TARGET_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), Collections.singletonMap("value", "1.0"));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_RESOLVED.getName());
		Map<String, Object> expectedCustomFieldValue = idValueMap(10, "v1");
		assertThat(updatedSourceIssue.getFields().getOther()).containsExactly(entry(SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), expectedCustomFieldValue));

		syncAndAssertNoChanges();
	}

	@Test
	public void testDoNotTriggerTransitionAfterTicketWasMovedBetweenProjects() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();

		moveTicketForwardAndBack(createdSourceIssue.getKey());

		JiraIssue targetIssue = getSingleIssue(TARGET);
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		// when
		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_OPEN.getName());

		syncAndAssertNoChanges();
	}

	private void moveTicketForwardAndBack(String issueKey) {
		jiraDummyService.moveIssue(SOURCE, issueKey, SOURCE_PROJECT_OTHER.getKey());
		jiraDummyService.moveIssue(SOURCE, issueKey, SOURCE_PROJECT.getKey());

		JiraIssue sourceIssueMovedBack = getSingleIssue(SOURCE);
		assertThat(sourceIssueMovedBack.getKey()).isNotEqualTo(issueKey);
	}

	@Test
	@DirtiesContext
	public void testTriggerTransitionAfterTicketWasMovedBetweenProjects() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();

		moveTicketForwardAndBack(createdSourceIssue.getKey());

		JiraIssue targetIssue = getSingleIssue(TARGET);
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncConfig.getProjects().get("PRJ_ONE").getTransition("ResolveWhenClosed").setTriggerIfIssueWasMovedBetweenProjects(true);

		// when
		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_RESOLVED.getName());

		syncAndAssertNoChanges();
	}

	private JiraIssue createIssueInSource() {
		return createIssueInSource(SOURCE_TYPE_BUG);
	}

	private JiraIssue createIssueInSource(JiraIssueType issueType) {
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(issueType);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);

		return jiraSource.createIssue(sourceIssue);
	}

	private JiraTransition findTransition(Context context, String issueKey, JiraIssueStatus statusToTransitionTo) {
		List<JiraTransition> transitions = jiraDummyService.getTransitions(context, issueKey).getTransitions();
		List<JiraTransition> filteredTransitions = transitions.stream()
			.filter(transition -> transition.getTo().getName().equals(statusToTransitionTo.getName()))
			.collect(Collectors.toList());
		assertThat(filteredTransitions).hasSize(1);
		return filteredTransitions.iterator().next();
	}

	@Test
	public void testSetTicketToInProgressInSourceWhenTargetGetsAssigned() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();
		syncAndAssertNoChanges();

		JiraIssue currentTargetIssue = getSingleIssue(TARGET);

		assertThat(currentTargetIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_OPEN.getName());

		// when

		JiraIssue targetIssue = getSingleIssue(TARGET);
		targetIssue.getFields().setAssignee(new JiraUser("some", "body"));

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = jiraDummyService.getIssueByKey(SOURCE, createdSourceIssue.getKey());

		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_IN_PROGRESS.getName());
		assertThat(updatedSourceIssue.getFields().getAssignee().getKey()).isEqualTo("myself");

		syncAndAssertNoChanges();
	}

	@Test
	public void testSetTicketToReopenedInTargetWhenSourceIsReopened() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource(SOURCE_TYPE_BUG);

		syncAndCheckResult();
		syncAndAssertNoChanges();

		JiraIssue targetIssue = getSingleIssue(TARGET);

		transitionIssue(TARGET, targetIssue, TARGET_STATUS_CLOSED);

		// when
		transitionIssue(SOURCE, createdSourceIssue, SOURCE_STATUS_REOPENED);

		syncAndCheckResult();

		// then
		JiraIssue updatedTargetIssue = getSingleIssue(TARGET);

		assertThat(updatedTargetIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_REOPENED.getName());

		syncAndAssertNoChanges();
	}

	private void transitionIssue(Context context, JiraIssue targetIssue, JiraIssueStatus statusToTransitionTo) {
		JiraTransition transition = findTransition(context, targetIssue.getKey(), statusToTransitionTo);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		jiraDummyService.transitionIssue(context, targetIssue.getKey(), update);
	}

	@Test
	public void testCreateTicketInTarget_WithComments() throws Exception {
		// given
		JiraIssue createdIssue = createIssueInSource(SOURCE_TYPE_UNKNOWN);
		jiraSource.addComment(createdIssue.getKey(), "some comment");
		clock.windForwardSeconds(120);
		jiraSource.addComment(createdIssue.getKey(), "some other comment");

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_TASK.getName());
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(2);

		assertThat(comments.iterator().next().getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		assertThat(comments.get(1).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:02:00 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some other comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicket_UsernameReferences() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setDescription("mentioning [~" + SOURCE_USER_SOME.getName() + "] in description");
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		JiraIssue createdIssue = jiraSource.createIssue(sourceIssue);

		jiraSource.addComment(createdIssue.getKey(), "[~" + SOURCE_USER_ANOTHER.getName() + "]: some comment");
		jiraSource.addComment(createdIssue.getKey(), "[~yetanotheruser]: some comment");

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"mentioning [Some User|https://localhost:" + port + "/SOURCE/secure/ViewProfile.jspa?name=some.user] in description\n" +
			"{panel}\n\n");
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(2);

		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"[Another User|https://localhost:" + port + "/SOURCE/secure/ViewProfile.jspa?name=anotheruser]: some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		assertThat(comments.get(1).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"[~yetanotheruser]: some comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicket_TicketReferences() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setDescription("mentioning " + SOURCE_PROJECT.getKey() + "-123 in description");
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		JiraIssue createdIssue = jiraSource.createIssue(sourceIssue);

		jiraSource.addComment(createdIssue.getKey(), "see ticket " + SOURCE_PROJECT.getKey() + "-456");

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"mentioning [PROJECT_ONE-123|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-123] in description\n" +
			"{panel}\n\n");

		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);

		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"see ticket [PROJECT_ONE-456|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-456]\n" +
			"~??[comment 1_1|https://localhost:" + this.port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testUpdateTicketInTarget_addComment() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment()).isNull();

		// when
		clock.windForwardSeconds(30);

		jiraSource.addComment(createdSourceIssue.getKey(), "some comment");

		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);
		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:30 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testUpdateTicketInTarget_addCommentWithLinkToOtherComment() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment()).isNull();

		// when
		clock.windForwardSeconds(30);

		jiraSource.addComment(createdSourceIssue.getKey(), "some comment");

		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);
		String linkToFirstComment = "https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1";
		assertThat(comments.get(0).getBody()).contains(linkToFirstComment);

		syncAndAssertNoChanges();

		clock.windForwardSeconds(30);

		jiraSource.addComment(createdSourceIssue.getKey(), "see the [first comment|" + linkToFirstComment + "]");

		syncAndCheckResult();

		targetIssue = getSingleIssue(TARGET);
		comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(2);

		syncAndAssertNoChanges();

		targetIssue = getSingleIssue(TARGET);
		comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(2);
	}

	private List<ProjectSyncResult> syncAndCheckResult() {
		List<ProjectSyncResult> results = syncTask.sync();
		assertThat(results).hasSize(1);
		assertThat(results.get(0).hasFailed()).isFalse();
		return results;
	}

	private void syncAndAssertNoChanges() {
		List<ProjectSyncResult> results = syncAndCheckResult();
		for (ProjectSyncResult result : results) {
			for (SyncResult syncResult : SyncResult.values()) {
				switch (syncResult) {
					case UNCHANGED:
						break;
					case UNCHANGED_WARNING:
					case CHANGED:
					case CHANGED_TRANSITION:
					case FAILED:
					case CREATED:
						assertThat(result.getCount(syncResult)).as("number of " + syncResult + " issues").isZero();
						break;
					default:
						throw new IllegalArgumentException("Unknown syncResult: " + syncResult);
				}
			}
		}
	}

	@Test
	public void testUpdateTicketInTarget_updateCustomField() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Collections.singletonList("1.0"));

		JiraIssue createdSourceIssue = jiraSource.createIssue(sourceIssue);

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment()).isNull();

		// when
		clock.windForwardSeconds(30);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.getOrCreateFields().setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1"));
		jiraSource.updateIssue(createdSourceIssue.getKey(), update);

		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getOther()).containsExactly(entry(TARGET_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1")));

		syncAndAssertNoChanges();
	}

	@Test
	public void testUpdateCommentInSource() throws Exception {
		JiraIssue createdSourceIssue = createIssueInSource();
		jiraSource.addComment(createdSourceIssue.getKey(), "first comment");
		JiraComment secondComment = jiraSource.addComment(createdSourceIssue.getKey(), "second comment");

		clock.windForwardSeconds(30);

		ZonedDateTime firstSyncTime = ZonedDateTime.now(clock);

		syncAndCheckResult();

		clock.windForwardSeconds(30);

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment().getComments()).hasSize(2);

		jiraSource.updateComment(createdSourceIssue.getKey(), secondComment.getId(), "updated second comment");

		clock.windForwardSeconds(30);

		ZonedDateTime secondSyncTime = ZonedDateTime.now(clock);

		syncAndCheckResult();

		targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment().getComments()).hasSize(2);
		JiraComment comment = targetIssue.getFields().getComment().getComments().get(1);
		assertThat(comment.getCreated()).isEqualTo(firstSyncTime);
		assertThat(comment.getUpdated()).isEqualTo(secondSyncTime);
		assertThat(comment.getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST (Updated: 2016-05-23 20:01:00 CEST)|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"updated second comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/PROJECT_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicketInTargetWithComment_UpdateDoesNotAddItAgain() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();
		jiraSource.addComment(createdSourceIssue.getKey(), "some comment");

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment().getComments()).hasSize(1);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);

		syncAndAssertNoChanges();
	}

	@Test
	public void testUpdateTicketInTarget_addCommentAfterCommentWasAddedInTarget() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		jiraSource.addComment(createdSourceIssue.getKey(), "first comment in source");

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment().getComments()).hasSize(1);

		clock.windForwardSeconds(30);

		jiraSource.addComment(createdSourceIssue.getKey(), "second comment in source");

		clock.windForwardSeconds(30);

		jiraSource.addComment(createdSourceIssue.getKey(), "third comment in source");

		clock.windForwardSeconds(30);

		jiraTarget.addComment(targetIssue.getKey(), "some comment in target");

		// when

		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(4);
		assertThat(comments.get(0).getBody()).contains("first comment in source").contains("2016-05-23 20:00:00 CEST|titleBGColor=#DDD|bgColor=#EEE");
		assertThat(comments.get(1).getBody()).contains("some comment in target");

		assertThat(comments.get(2).getBody())
			.contains("second comment in source")
			.contains("2016-05-23 20:00:30 CEST|titleBGColor=#CCC|bgColor=#DDD")
			.contains("This comment was added behind time. The order of comments might not represent the real order.");

		assertThat(comments.get(3).getBody())
			.contains("third comment in source")
			.contains("2016-05-23 20:01:00 CEST|titleBGColor=#CCC|bgColor=#DDD")
			.contains("This comment was added behind time. The order of comments might not represent the real order.");

		syncAndAssertNoChanges();
	}

	@Test
	public void testDoNotUpdateTicketInStatusResolvedOrClosed() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource();

		JiraComment comment = jiraSource.addComment(createdSourceIssue.getKey(), "first comment in source");

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment().getComments()).hasSize(1);

		clock.windForwardSeconds(30);

		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_RESOLVED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		jiraSource.updateComment(createdSourceIssue.getKey(), comment.getId(), "changed comment in source");

		clock.windForwardSeconds(30);

		// when
		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);

		assertThat(targetIssue.getFields().getFixVersions()).containsExactly(TARGET_VERSION_10);

		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);
		assertThat(comments.get(0).getBody()).contains("first comment in source");

		syncAndAssertNoChanges();
	}

}
