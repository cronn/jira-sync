package de.cronn.jira.sync;

import static de.cronn.jira.sync.SetUtils.newLinkedHashSet;
import static de.cronn.jira.sync.config.Context.SOURCE;
import static de.cronn.jira.sync.config.Context.TARGET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import de.cronn.jira.sync.config.Context;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraComponent;
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
import de.cronn.jira.sync.dummy.JiraFilter;
import de.cronn.jira.sync.service.JiraService;
import de.cronn.jira.sync.strategy.SyncResult;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class JiraSyncApplicationTests {

	private static final JiraProject SOURCE_PROJECT_1 = new JiraProject("1", "SRC_ONE");
	private static final JiraProject TARGET_PROJECT_1 = new JiraProject("100", "TRG_ONE");

	private static final JiraProject SOURCE_PROJECT_2 = new JiraProject("2", "SRC_TWO");
	private static final JiraProject TARGET_PROJECT_2 = new JiraProject("200", "TRG_TWO");

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
	private static final JiraIssueType SOURCE_TYPE_STORY = new JiraIssueType("2", "Story");
	private static final JiraIssueType SOURCE_TYPE_UNKNOWN = new JiraIssueType("3", "Unknown");

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

	private static final JiraComponent SOURCE_COMPONENT_BACKEND = new JiraComponent("1", "Backend");
	private static final JiraComponent SOURCE_COMPONENT_FRONTEND = new JiraComponent("2", "Frontend");
	private static final JiraComponent SOURCE_COMPONENT_UNDEFINED = new JiraComponent("98", "Undefined");
	private static final JiraComponent SOURCE_COMPONENT_UNMAPPED = new JiraComponent("99", "Unmapped component");

	private static final JiraComponent TARGET_COMPONENT_TEAM_A = new JiraComponent("100", "Team A");
	private static final JiraComponent TARGET_COMPONENT_TEAM_B = new JiraComponent("101", "Team B");

	private static final JiraUser SOURCE_USER_SOME = new JiraUser("some.user", "some.user", "Some User");
	private static final JiraUser SOURCE_USER_ANOTHER = new JiraUser("anotheruser", "anotheruser", "Another User");

	private static final JiraFieldSchema FIELD_SCHEMA_LABELS = new JiraFieldSchema(null, null, "com.atlassian.jira.plugin.system.customfieldtypes:labels");
	private static final JiraFieldSchema FIELD_SCHEMA_SELECT = new JiraFieldSchema(null, null, "com.atlassian.jira.plugin.system.customfieldtypes:select");

	private static final JiraField SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION = new JiraField("1", "Found in version", true, FIELD_SCHEMA_LABELS);
	private static final JiraField TARGET_CUSTOM_FIELD_FOUND_IN_VERSION = new JiraField("100", "Found in software version", true, FIELD_SCHEMA_LABELS);

	private static final JiraField SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION = new JiraField("2", "Fixed in version", true, FIELD_SCHEMA_SELECT);
	private static final JiraField TARGET_CUSTOM_FIELD_FIXED_IN_VERSION = new JiraField("200", "Fixed in software version", true, FIELD_SCHEMA_SELECT);

	private static final String SOURCE_PROJECT_1_FILTER_ID_1 = "12345";
	private static final String SOURCE_PROJECT_1_FILTER_ID_2 = "56789";
	private static final String SOURCE_PROJECT_2_FILTER_ID = "2222";

	@Autowired
	private TestClock clock;

	@SpyBean
	private JiraDummyService jiraDummyService;

	@Autowired
	private JiraService jiraSource;

	@Autowired
	private JiraService jiraTarget;

	@Autowired
	private JiraSyncTask syncTask;

	@Autowired
	private JiraSyncConfig syncConfig;

	@Autowired
	private StoringRequestFilter storingRequestFilter;

	@LocalServerPort
	private int port;

	private String sourceBaseUrl;

	private String targetBaseUrl;

	@TestConfiguration
	static class TestConfig {
		@Bean
		public CommonsRequestLoggingFilter requestLoggingFilter() {
			CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
			filter.setIncludeQueryString(true);
			filter.setIncludePayload(true);
			filter.setMaxPayloadLength(10000);
			filter.setIncludeHeaders(false);
			filter.setAfterMessagePrefix("REQUEST DATA : ");
			return filter;
		}

		@Bean
		public StoringRequestFilter storingRequestFilter() {
			return new StoringRequestFilter();
		}
	}

	private static JiraFilter filterByProjectAndTypes(JiraProject project, JiraIssueType... issueTypes) {
		return issue -> {
			JiraIssueFields fields = issue.getFields();
			return fields.getProject().getId().equals(project.getId())
				&& Arrays.stream(issueTypes).anyMatch(issueType -> fields.getIssuetype().getId().equals(issueType.getId()));
		};
	}

	@Before
	public void resetClock() {
		clock.reset();
	}

	@Before
	public void setUp() throws Exception {
		String commonBaseUrl = "https://localhost:" + port + "/";
		sourceBaseUrl = commonBaseUrl + SOURCE + "/";
		targetBaseUrl = commonBaseUrl + TARGET + "/";

		syncConfig.getSource().setUrl(sourceBaseUrl);
		syncConfig.getTarget().setUrl(targetBaseUrl);

		jiraDummyService.reset();

		jiraSource.evictAllCaches();
		jiraTarget.evictAllCaches();

		jiraDummyService.setBaseUrl(SOURCE, sourceBaseUrl);
		jiraDummyService.setBaseUrl(TARGET, targetBaseUrl);

		jiraDummyService.addProject(SOURCE, SOURCE_PROJECT_1);
		jiraDummyService.addProject(TARGET, TARGET_PROJECT_1);

		jiraDummyService.addProject(SOURCE, SOURCE_PROJECT_2);
		jiraDummyService.addProject(TARGET, TARGET_PROJECT_2);

		jiraDummyService.setFilter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_1, filterByProjectAndTypes(SOURCE_PROJECT_1, SOURCE_TYPE_BUG, SOURCE_TYPE_UNKNOWN));
		jiraDummyService.setFilter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_2, issue -> false);
		jiraDummyService.associateFilterIdToProject(SOURCE, SOURCE_PROJECT_2_FILTER_ID, SOURCE_PROJECT_2);

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

		jiraDummyService.addComponent(SOURCE, SOURCE_COMPONENT_BACKEND);
		jiraDummyService.addComponent(SOURCE, SOURCE_COMPONENT_FRONTEND);
		jiraDummyService.addComponent(SOURCE, SOURCE_COMPONENT_UNDEFINED);
		jiraDummyService.addComponent(SOURCE, SOURCE_COMPONENT_UNMAPPED);

		jiraDummyService.addComponent(TARGET, TARGET_COMPONENT_TEAM_A);
		jiraDummyService.addComponent(TARGET, TARGET_COMPONENT_TEAM_B);

		jiraDummyService.addUser(SOURCE, SOURCE_USER_SOME);
		jiraDummyService.addUser(SOURCE, SOURCE_USER_ANOTHER);

		jiraDummyService.addField(SOURCE, SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION, null);
		jiraDummyService.addField(TARGET, TARGET_CUSTOM_FIELD_FOUND_IN_VERSION, null);

		jiraDummyService.addField(SOURCE, SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION, Collections.singletonMap("v1", 10L));
		jiraDummyService.addField(TARGET, TARGET_CUSTOM_FIELD_FIXED_IN_VERSION, Collections.singletonMap("1.0", 100L));

		jiraDummyService.setDefaultStatus(TARGET, TARGET_STATUS_OPEN);

		TARGET_PROJECT_1.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));
		TARGET_PROJECT_2.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));

		jiraDummyService.expectLoginRequest(SOURCE, "jira-sync", "secret in source");
		jiraDummyService.expectLoginRequest(TARGET, "jira-sync", "secret in target");

		jiraDummyService.expectBasicAuth(TARGET, "basic-auth-user", "secret");

		jiraSource.login(syncConfig.getSource(), true);
		jiraTarget.login(syncConfig.getTarget(), false);

		storingRequestFilter.clear();
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

		assertThat(resolutionMapping).containsOnly(
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
	public void testFetchingFilterOfOneProjectFails() throws Exception {
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_2);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		jiraSource.createIssue(sourceIssue);

		doThrow(new RuntimeException("Illegal filter")).when(jiraDummyService).filter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_1);

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> syncTask.sync())
			.withMessage("Synchronisation failed for: [SRC_ONE -> TRG_ONE]");

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getSummary()).isEqualTo("SRC_TWO-1: some bug");

		verify(jiraDummyService).filter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_1);
		verify(jiraDummyService).filter(SOURCE, SOURCE_PROJECT_2_FILTER_ID);
	}

	@Test
	public void testCreateTicketInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setLabels(newLinkedHashSet("label1", "label2"));
		sourceIssue.getFields().setVersions(newLinkedHashSet(SOURCE_VERSION_10, SOURCE_VERSION_11, SOURCE_VERSION_UNDEFINED));
		sourceIssue.getFields().setFixVersions(newLinkedHashSet(SOURCE_VERSION_11));
		sourceIssue.getFields().setComponents(newLinkedHashSet(SOURCE_COMPONENT_BACKEND, SOURCE_COMPONENT_FRONTEND, SOURCE_COMPONENT_UNMAPPED));
		JiraIssue createdSourceIssue = jiraSource.createIssue(sourceIssue);

		clock.windForwardSeconds(30);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		JiraIssueFields targetIssueFields = targetIssue.getFields();
		assertThat(targetIssueFields.getSummary()).isEqualTo("SRC_ONE-1: My first bug");
		assertThat(targetIssueFields.getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssueFields.getPriority().getName()).isEqualTo(TARGET_PRIORITY_CRITICAL.getName());
		assertThat(targetIssueFields.getLabels()).containsExactly("label1", "label2");
		assertThat(targetIssueFields.getVersions()).extracting(JiraVersion::getName).containsExactlyInAnyOrder("10", "11");
		assertThat(targetIssueFields.getFixVersions()).extracting(JiraVersion::getName).containsExactly("11");
		assertThat(targetIssueFields.getComponents()).extracting(JiraComponent::getName).containsExactlyInAnyOrder("Team A", "Team B");
		assertThat(targetIssueFields.getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		List<JiraRemoteLink> remoteLinksInTarget = jiraDummyService.getRemoteLinks(TARGET, targetIssue);
		List<JiraRemoteLink> remoteLinksInSource = jiraDummyService.getRemoteLinks(SOURCE, createdSourceIssue);
		assertThat(remoteLinksInTarget).hasSize(1);
		assertThat(remoteLinksInSource).hasSize(1);

		JiraRemoteLinkObject firstRemoteLinkInSource = remoteLinksInSource.iterator().next().getObject();
		assertThat(firstRemoteLinkInSource.getUrl()).isEqualTo(new URL(targetBaseUrl + "/browse/TRG_ONE-1"));
		assertThat(firstRemoteLinkInSource.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-source/favicon.ico"));

		JiraRemoteLinkObject firstRemoteLinkInTarget = remoteLinksInTarget.iterator().next().getObject();
		assertThat(firstRemoteLinkInTarget.getUrl()).isEqualTo(new URL(sourceBaseUrl + "/browse/SRC_ONE-1"));
		assertThat(firstRemoteLinkInTarget.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-target/favicon.ico"));

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicketInTargetFromSecondFilter() throws Exception {
		JiraIssue sourceIssue1 = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue1.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue1.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue1.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue1.getFields().setLabels(newLinkedHashSet("label1", "label2"));
		sourceIssue1.getFields().setVersions(newLinkedHashSet(SOURCE_VERSION_10, SOURCE_VERSION_11, SOURCE_VERSION_UNDEFINED));
		sourceIssue1.getFields().setFixVersions(newLinkedHashSet(SOURCE_VERSION_11));
		JiraIssue createdSourceIssue1 = jiraSource.createIssue(sourceIssue1);

		JiraIssue sourceIssue2 = new JiraIssue(null, null, "My second bug", SOURCE_STATUS_OPEN);
		sourceIssue2.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue2.getFields().setIssuetype(SOURCE_TYPE_STORY);
		sourceIssue2.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue2.getFields().setLabels(newLinkedHashSet("label1", "label2"));
		sourceIssue2.getFields().setVersions(newLinkedHashSet(SOURCE_VERSION_10, SOURCE_VERSION_11, SOURCE_VERSION_UNDEFINED));
		sourceIssue2.getFields().setFixVersions(newLinkedHashSet(SOURCE_VERSION_11));
		jiraSource.createIssue(sourceIssue2);

		clock.windForwardSeconds(30);
		syncAndCheckResult();

		JiraIssue targetIssue1 = getSingleIssue(TARGET);
		JiraIssueFields targetIssueFields1 = targetIssue1.getFields();
		assertThat(targetIssueFields1.getSummary()).isEqualTo("SRC_ONE-1: My first bug");

		jiraDummyService.setFilter(SOURCE, "56789", filterByProjectAndTypes(SOURCE_PROJECT_1, SOURCE_TYPE_STORY));
		clock.windForwardSeconds(30);
		syncAndCheckResult();

		JiraIssue targetIssue2 = jiraDummyService.getIssueByKey(TARGET, "TRG_ONE-2");
		JiraIssueFields targetIssueFields2 = targetIssue2.getFields();
		assertThat(targetIssueFields2.getSummary()).isEqualTo("SRC_ONE-2: My second bug");

		// Let the filters overlap
		jiraDummyService.setFilter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_1, filterByProjectAndTypes(SOURCE_PROJECT_1, SOURCE_TYPE_BUG));
		jiraDummyService.setFilter(SOURCE, SOURCE_PROJECT_1_FILTER_ID_2, filterByProjectAndTypes(SOURCE_PROJECT_1, SOURCE_TYPE_BUG));

		jiraSource.updateIssue(createdSourceIssue1.getKey(), fields -> fields.setDescription("changed description"));

		clock.windForwardSeconds(30);
		syncAndCheckResult();

		JiraIssue updatedTargetIssue1 = jiraDummyService.getIssueByKey(TARGET, targetIssue1.getKey());
		assertThat(updatedTargetIssue1.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"changed description\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testUnmappedResources() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_UNMAPPED);
		sourceIssue.getFields().setVersions(newLinkedHashSet(SOURCE_VERSION_UNMAPPED));
		sourceIssue.getFields().setComponents(newLinkedHashSet(SOURCE_COMPONENT_UNMAPPED));
		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssue.getFields().getPriority().getName()).isEqualTo(TARGET_PRIORITY_DEFAULT.getName());
		assertThat(targetIssue.getFields().getVersions()).isEmpty();
		assertThat(targetIssue.getFields().getComponents()).isEmpty();

		syncAndAssertNoChanges();
	}

	@Test
	public void testNotConfiguredVersions() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_2);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_UNMAPPED);
		sourceIssue.getFields().setVersions(null);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		String payload = storingRequestFilter.getPayload(HttpMethod.POST, "/TARGET/rest/api/2/issue");
		assertThat(payload).isNotEmpty();
		assertThat(payload).doesNotContain("\"versions\":[]");
		assertThat(payload).doesNotContain("\"fixVersions\":[]");
		assertThat(targetIssue.getFields().getVersions()).isNull();

		syncAndAssertNoChanges();
	}

	@Test
	public void testNotConfiguredComponents() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_2);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_UNMAPPED);
		sourceIssue.getFields().setComponents(null);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		String payload = storingRequestFilter.getPayload(HttpMethod.POST, "/TARGET/rest/api/2/issue");
		assertThat(payload).isNotEmpty();
		assertThat(payload).doesNotContain("\"components\":[]");
		assertThat(targetIssue.getFields().getComponents()).isNull();

		syncAndAssertNoChanges();
	}

	private JiraIssue getSingleIssue(Context context) {
		Set<JiraIssue> issues = jiraDummyService.getAllIssues(context);
		assertThat(issues).hasSize(1);
		return issues.iterator().next();
	}

	@Test
	public void testErrorHandling() throws Exception {
		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> jiraSource.createIssue(new JiraIssue()))
			.withMessage("[https://localhost:" + port + "/SOURCE/] Bad Request: fields are missing");
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
	public void testCreateTicketInTarget_WithCustomFields() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1"));
		sourceIssue.getFields().setOther(SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), Collections.singletonMap("value", "v1"));

		jiraSource.createIssue(sourceIssue);

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_TASK.getName());
		Map<String, Object> other = targetIssue.getFields().getOther();

		assertThat(other).containsExactly(
			entry(TARGET_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1")),
			entry(TARGET_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), idValueMap(100, "1.0"))
		);

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
		jiraSource.updateIssue(createdSourceIssue.getKey(), fields -> fields.setDescription("changed description"));

		Instant beforeSecondUpdate = Instant.now(clock);
		clock.windForwardSeconds(30);

		syncAndCheckResult();

		// then
		targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nchanged description\n{panel}");
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
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);
		assertThat(updatedSourceIssue.getFields().getResolution()).isEqualTo(SOURCE_RESOLUTION_FIXED);
		assertThat(updatedSourceIssue.getFields().getFixVersions()).containsExactly(SOURCE_VERSION_10);

		syncAndAssertNoChanges();
	}

	@Test
	public void testSetTicketToResolvedInSourceWhenTargetTicketIsClosed_KeepUnmappedVersionInSource() throws Exception {
		// given
		JiraIssue issueInSource = createIssueInSource();
		JiraIssueUpdate updateSourceVersion = new JiraIssueUpdate();
		updateSourceVersion.getOrCreateFields().setFixVersions(newLinkedHashSet(SOURCE_VERSION_UNMAPPED));
		jiraDummyService.updateIssue(SOURCE, issueInSource.getKey(), updateSourceVersion);

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);

		// when
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);
		assertThat(updatedSourceIssue.getFields().getResolution()).isEqualTo(SOURCE_RESOLUTION_FIXED);
		assertThat(updatedSourceIssue.getFields().getFixVersions()).containsExactly(SOURCE_VERSION_UNMAPPED, SOURCE_VERSION_10);

		syncAndAssertNoChanges();
	}

	@Test
	public void testCopyCustomFieldsWhenIssueIsClosed() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
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
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		update.getOrCreateFields().setOther(TARGET_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), Collections.singletonMap("value", "1.0"));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);
		Map<String, Object> expectedCustomFieldValue = idValueMap(10, "v1");
		assertThat(updatedSourceIssue.getFields().getOther()).containsExactly(entry(SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), expectedCustomFieldValue));

		syncAndAssertNoChanges();
	}

	@Test
	public void testCopyCustomFieldsWhenIssueIsClosed_Feature() throws Exception {
		// given
		createIssueInSource(SOURCE_TYPE_UNKNOWN);

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);

		// when
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		update.getOrCreateFields().setOther(TARGET_CUSTOM_FIELD_FIXED_IN_VERSION.getId(), Collections.singletonMap("value", "1.0"));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);
		assertThat(updatedSourceIssue.getFields().getOther()).isEmpty();

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
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		// when
		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_OPEN);

		syncAndAssertNoChanges();
	}

	private void moveTicketForwardAndBack(String issueKey) {
		jiraDummyService.moveIssue(SOURCE, issueKey, SOURCE_PROJECT_2.getKey());
		jiraDummyService.moveIssue(SOURCE, issueKey, SOURCE_PROJECT_1.getKey());

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
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncConfig.getProjects().get(TARGET_PROJECT_1.getKey()).getTransition("ResolveWhenClosed").setTriggerIfIssueWasMovedBetweenProjects(true);

		// when
		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = getSingleIssue(SOURCE);
		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);

		syncAndAssertNoChanges();
	}

	private JiraIssue createIssueInSource() {
		return createIssueInSource(SOURCE_TYPE_BUG);
	}

	private JiraIssue createIssueInSource(JiraIssueType issueType) {
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
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

		assertThat(currentTargetIssue.getFields().getStatus()).isEqualTo(TARGET_STATUS_OPEN);

		// when

		JiraIssue targetIssue = getSingleIssue(TARGET);
		targetIssue.getFields().setAssignee(new JiraUser("some", "body"));

		syncAndCheckResult();

		// then
		JiraIssue updatedSourceIssue = jiraDummyService.getIssueByKey(SOURCE, createdSourceIssue.getKey());

		assertThat(updatedSourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_IN_PROGRESS);
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

		assertThat(updatedTargetIssue.getFields().getStatus()).isEqualTo(TARGET_STATUS_REOPENED);

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

		assertThat(comments.iterator().next().getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		assertThat(comments.get(1).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:02:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"some other comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicket_UsernameReferences() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setDescription("mentioning [~" + SOURCE_USER_SOME.getName() + "] in description");
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		JiraIssue createdIssue = jiraSource.createIssue(sourceIssue);

		jiraSource.addComment(createdIssue.getKey(), "[~" + SOURCE_USER_ANOTHER.getName() + "]: some comment");
		jiraSource.addComment(createdIssue.getKey(), "[~yetanotheruser]: some comment");

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"mentioning [Some User|https://localhost:" + port + "/SOURCE/secure/ViewProfile.jspa?name=some.user] in description\n" +
			"{panel}\n\n");
		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(2);

		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"[Another User|https://localhost:" + port + "/SOURCE/secure/ViewProfile.jspa?name=anotheruser]: some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
			"{panel}");

		assertThat(comments.get(1).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"[~yetanotheruser]: some comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
			"{panel}");

		syncAndAssertNoChanges();
	}

	@Test
	public void testCreateTicket_TicketReferences() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setDescription("mentioning " + SOURCE_PROJECT_1.getKey() + "-123 in description");
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		JiraIssue createdIssue = jiraSource.createIssue(sourceIssue);

		jiraSource.addComment(createdIssue.getKey(), "see ticket " + SOURCE_PROJECT_1.getKey() + "-456");

		// when
		syncAndCheckResult();

		// then
		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"mentioning [SRC_ONE-123|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-123] in description\n" +
			"{panel}\n\n");

		List<JiraComment> comments = targetIssue.getFields().getComment().getComments();
		assertThat(comments).hasSize(1);

		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"see ticket [SRC_ONE-456|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-456]\n" +
			"~??[comment 1_1|https://localhost:" + this.port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
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
		assertThat(comments.get(0).getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:30 CEST|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"some comment\n" +
			"~??[comment 1_1|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1]??~\n" +
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
		String linkToFirstComment = "https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_1";
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
		assertThat(results).extracting(ProjectSyncResult::hasFailed).containsExactly(false, false);
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
		sourceIssue.getFields().setProject(SOURCE_PROJECT_1);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Collections.singletonList("1.0"));

		JiraIssue createdSourceIssue = jiraSource.createIssue(sourceIssue);

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		assertThat(targetIssue.getFields().getComment()).isNull();

		// when
		clock.windForwardSeconds(30);

		jiraSource.updateIssue(createdSourceIssue.getKey(), fields -> {
			fields.setOther(SOURCE_CUSTOM_FIELD_FOUND_IN_VERSION.getId(), Arrays.asList("1.0", "1.1"));
		});

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
		assertThat(comment.getBody()).isEqualTo("{panel:title=my self - 2016-05-23 20:00:00 CEST (Updated: 2016-05-23 20:01:00 CEST)|titleBGColor=#dddddd|bgColor=#eeeeee}\n" +
			"updated second comment\n" +
			"~??[comment 1_2|https://localhost:" + port + "/SOURCE/browse/SRC_ONE-1?focusedCommentId=1_2&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1_2]??~\n" +
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
		assertThat(comments.get(0).getBody()).contains("first comment in source").contains("2016-05-23 20:00:00 CEST|titleBGColor=#dddddd|bgColor=#eeeeee");
		assertThat(comments.get(1).getBody()).contains("some comment in target");

		assertThat(comments.get(2).getBody())
			.contains("second comment in source")
			.contains("2016-05-23 20:00:30 CEST|titleBGColor=#cccccc|bgColor=#dddddd")
			.contains("This comment was added behind time. The order of comments might not represent the real order.");

		assertThat(comments.get(3).getBody())
			.contains("third comment in source")
			.contains("2016-05-23 20:01:00 CEST|titleBGColor=#cccccc|bgColor=#dddddd")
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
		update.getOrCreateFields().setFixVersions(newLinkedHashSet(TARGET_VERSION_10));
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

	@Test
	public void testGetAllowedValuesForCustomField() {
		Map<String, Object> fields = jiraSource.getAllowedValuesForCustomField(SOURCE_PROJECT_1.getKey(), SOURCE_CUSTOM_FIELD_FIXED_IN_VERSION.getId());
		assertThat(fields).isNotNull();
		assertThat(fields.values().size()).isGreaterThanOrEqualTo(1);
	}

	@Test
	public void testDoNotResolveTicketInSourceWhenReopened() throws Exception {
		createIssueInSource();

		syncAndCheckResult();

		JiraIssue targetIssue = getSingleIssue(TARGET);
		transitionIssue(TARGET, targetIssue, TARGET_STATUS_CLOSED);

		syncAndCheckResult();

		JiraIssue sourceIssue = getSingleIssue(SOURCE);
		assertThat(sourceIssue.getFields().getStatus()).isEqualTo(SOURCE_STATUS_RESOLVED);

		clock.windForwardSeconds(60);

		transitionIssue(SOURCE, sourceIssue, SOURCE_STATUS_REOPENED);
		transitionIssue(SOURCE, sourceIssue, SOURCE_STATUS_IN_PROGRESS);

		syncAndAssertNoChanges();
	}

}
