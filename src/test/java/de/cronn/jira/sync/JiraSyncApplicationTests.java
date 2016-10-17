package de.cronn.jira.sync;

import static de.cronn.jira.sync.dummy.JiraDummyService.Context.*;
import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
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

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("/test.properties")
public class JiraSyncApplicationTests {

	private static final JiraProject SOURCE_PROJECT = new JiraProject("1", "PROJECT_ONE");
	private static final JiraProject TARGET_PROJECT = new JiraProject("100", "PRJ_ONE");

	private static final JiraIssueStatus SOURCE_STATUS_OPEN = new JiraIssueStatus("1", "Open");
	private static final JiraIssueStatus SOURCE_STATUS_IN_PROGRESS = new JiraIssueStatus("2", "In Progress");
	private static final JiraIssueStatus SOURCE_STATUS_RESOLVED = new JiraIssueStatus("3", "Resolved");
	private static final JiraIssueStatus TARGET_STATUS_OPEN = new JiraIssueStatus("100", "Open");
	private static final JiraIssueStatus TARGET_STATUS_CLOSED = new JiraIssueStatus("102", "Closed");

	private static final JiraIssueType SOURCE_TYPE_BUG = new JiraIssueType("1", "Bug");
	private static final JiraIssueType SOURCE_TYPE_UNKNOWN = new JiraIssueType("2", "Unknown");

	private static final JiraIssueType TARGET_TYPE_BUG = new JiraIssueType("100", "Bug");
	private static final JiraIssueType TARGET_TYPE_IMPROVEMENT = new JiraIssueType("101", "Improvement");
	private static final JiraIssueType TARGET_TYPE_TASK = new JiraIssueType("102", "Task");

	private static final JiraPriority SOURCE_PRIORITY_HIGH = new JiraPriority("1", "High");
	private static final JiraPriority SOURCE_PRIORITY_UNMAPPED = new JiraPriority("99", "Unmapped priority");

	private static final JiraPriority TARGET_PRIORITY_CRITICAL = new JiraPriority("100", "Critical");

	private static final JiraResolution SOURCE_RESOLUTION_FIXED = new JiraResolution("1", "Fixed");
	private static final JiraResolution TARGET_RESOLUTION_DONE = new JiraResolution("100", "Done");

	private static final JiraVersion SOURCE_VERSION_10 = new JiraVersion("1", "10.0");
	private static final JiraVersion SOURCE_VERSION_11 = new JiraVersion("2", "11.0");
	private static final JiraVersion SOURCE_VERSION_UNDEFINED = new JiraVersion("98", "Undefined");
	private static final JiraVersion SOURCE_VERSION_UNMAPPED = new JiraVersion("99", "Unmapped version");

	private static final JiraVersion TARGET_VERSION_10 = new JiraVersion("100", "10");
	private static final JiraVersion TARGET_VERSION_11 = new JiraVersion("101", "11");

	@Autowired
	private TestClock clock;

	@Autowired
	private JiraDummyService jiraDummyService;

	@Autowired
	private JiraService jiraSource;

	@Autowired
	private JiraService jiraTarget;

	@Autowired
	private JiraSyncConfig jiraSyncConfig;

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
		jiraDummyService.addProject(TARGET, TARGET_PROJECT);

		jiraDummyService.associateFilterIdToProject(SOURCE, "12345", SOURCE_PROJECT);

		jiraDummyService.addPriority(SOURCE, SOURCE_PRIORITY_HIGH);
		jiraDummyService.addPriority(SOURCE, SOURCE_PRIORITY_UNMAPPED);
		jiraDummyService.addPriority(TARGET, TARGET_PRIORITY_CRITICAL);

		jiraDummyService.addResolution(SOURCE, SOURCE_RESOLUTION_FIXED);
		jiraDummyService.addResolution(TARGET, TARGET_RESOLUTION_DONE);

		jiraDummyService.addTransition(SOURCE, new JiraTransition("1", "Set resolved", SOURCE_STATUS_RESOLVED));
		jiraDummyService.addTransition(SOURCE, new JiraTransition("2", "Set in progress", SOURCE_STATUS_IN_PROGRESS));

		jiraDummyService.addTransition(TARGET, new JiraTransition("100", "Close", TARGET_STATUS_CLOSED));

		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_10);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_11);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_UNDEFINED);
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_UNMAPPED);

		jiraDummyService.addVersion(TARGET, TARGET_VERSION_10);
		jiraDummyService.addVersion(TARGET, TARGET_VERSION_11);

		jiraDummyService.setDefaultStatus(TARGET, TARGET_STATUS_OPEN);

		TARGET_PROJECT.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));

		jiraDummyService.expectLoginRequest(SOURCE, "jira-sync", "secret in source");
		jiraDummyService.expectLoginRequest(TARGET, "jira-sync", "secret in target");

		jiraDummyService.expectBasicAuth(TARGET, "basic-auth-user", "secret");
	}

	@Test
	public void testConfiguration() throws Exception {
		Map<String, String> resolutionMapping = syncConfig.getResolutionMapping();
		assertThat(resolutionMapping.keySet()).containsExactlyInAnyOrder("Fixed", "Duplicate", "Incomplete", "Won't Fix", "Won't Do", "Cannot Reproduce", "Done");

		assertThat(resolutionMapping).containsEntry("Fixed", "Fixed");
		assertThat(resolutionMapping).containsEntry("Done", "Fixed");
		assertThat(resolutionMapping).containsEntry("Cannot Reproduce", "Cannot Reproduce");
		assertThat(resolutionMapping).containsEntry("Won't Fix", "Won't Fix");
	}

	@Test
	public void testResolutionsAreCached() throws Exception {
		assertThat(jiraSource).isNotSameAs(jiraTarget);

		try {
			jiraSource.login(jiraSyncConfig.getSource());
			jiraTarget.login(jiraSyncConfig.getTarget());

			jiraSource.getResolutions();
			List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
			List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();
			assertThat(sourceResolutions1).isSameAs(sourceResolutions2);

			jiraTarget.getResolutions();
			List<JiraResolution> targetResolutions1 = jiraTarget.getResolutions();
			List<JiraResolution> targetResolutions2 = jiraTarget.getResolutions();
			assertThat(targetResolutions1).isSameAs(targetResolutions2);

			assertThat(sourceResolutions1).isNotSameAs(targetResolutions1);
		} finally {
			jiraSource.logout();
			jiraTarget.logout();
		}
	}

	@Test
	public void testResolutionsAndPrioritiesAreCached() throws Exception {
		try {
			jiraSource.login(jiraSyncConfig.getSource());

			jiraSource.getResolutions();
			List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
			List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();

			jiraSource.getPriorities();
			List<JiraPriority> sourcePriorities1 = jiraSource.getPriorities();
			List<JiraPriority> sourcePriorities2 = jiraSource.getPriorities();
			assertThat(sourceResolutions1).isSameAs(sourceResolutions2);
			assertThat(sourcePriorities1).isSameAs(sourcePriorities2);
			assertThat(sourcePriorities1).isNotSameAs(sourceResolutions1);
		} finally {
			jiraSource.logout();
		}
	}

	@Test
	public void testEmptySync() throws Exception {
		// when
		syncTask.sync();

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
		jiraDummyService.createIssue(SOURCE, sourceIssue);

		clock.windForwardSeconds(30);

		// when
		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		JiraIssueFields targetIssueFields = targetIssue.getFields();
		assertThat(targetIssueFields.getSummary()).isEqualTo("PROJECT_ONE-1: My first bug");
		assertThat(targetIssueFields.getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssueFields.getPriority().getName()).isEqualTo(TARGET_PRIORITY_CRITICAL.getName());
		assertThat(targetIssueFields.getLabels()).containsExactly("label1", "label2");
		assertThat(getNames(targetIssueFields.getVersions())).containsExactlyInAnyOrder("10", "11");
		assertThat(getNames(targetIssueFields.getFixVersions())).containsExactly("11");
		assertThat(targetIssueFields.getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		assertThat(jiraDummyService.getAllIssues(SOURCE)).hasSize(1);
		JiraIssue updatedSourceIssue = jiraDummyService.getAllIssues(SOURCE).get(0);
		assertThat(updatedSourceIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		List<JiraRemoteLink> remoteLinksInTarget = jiraDummyService.getRemoteLinks(TARGET, targetIssue);
		List<JiraRemoteLink> remoteLinksInSource = jiraDummyService.getRemoteLinks(SOURCE, sourceIssue);
		assertThat(remoteLinksInTarget).hasSize(1);
		assertThat(remoteLinksInSource).hasSize(1);

		JiraRemoteLinkObject firstRemoteLinkInSource = remoteLinksInSource.get(0).getObject();
		assertThat(firstRemoteLinkInSource.getUrl()).isEqualTo(new URL(targetBaseUrl + "/browse/PRJ_ONE-1"));
		assertThat(firstRemoteLinkInSource.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-source/favicon.ico"));

		JiraRemoteLinkObject firstRemoteLinkInTarget = remoteLinksInTarget.get(0).getObject();
		assertThat(firstRemoteLinkInTarget.getUrl()).isEqualTo(new URL(sourceBaseUrl + "/browse/PROJECT_ONE-1"));
		assertThat(firstRemoteLinkInTarget.getIcon().getUrl16x16()).isEqualTo(new URL("https://jira-target/favicon.ico"));
	}

	@Test
	public void testUnmappedVersionAndUnmappedPriority() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_UNMAPPED);
		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_UNMAPPED));
		jiraDummyService.createIssue(SOURCE, sourceIssue);

		// when
		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_BUG.getName());
		assertThat(targetIssue.getFields().getPriority()).isNull();
		assertThat(targetIssue.getFields().getVersions()).isEmpty();
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
	public void testCreateTicketInTarget_WithFallbackType() throws Exception {
		// given
		createIssueInSource("some issue", SOURCE_TYPE_UNKNOWN);

		// when
		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_TYPE_TASK.getName());
	}

	@Test
	public void testUpdateTicketInTarget() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("");

		// when
		JiraIssueUpdate update = new JiraIssueUpdate();
		update.getOrCreateFields().setDescription("changed description");
		jiraDummyService.updateIssue(SOURCE, createdSourceIssue.getKey(), update);

		Instant beforeSecondUpdate = Instant.now(clock);
		clock.windForwardSeconds(30);

		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getDescription()).isEqualTo("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged description\n{panel}");
		assertThat(targetIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.now(clock));

		assertThat(jiraDummyService.getAllIssues(SOURCE)).hasSize(1);
		JiraIssue sourceIssue = jiraDummyService.getAllIssues(SOURCE).get(0);
		assertThat(sourceIssue.getFields().getUpdated().toInstant()).isEqualTo(beforeSecondUpdate);
	}

	@Test
	public void testSetTicketToResolvedInSourceWhenTargetTicketIsClosed() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);

		// when
		JiraTransition transition = findTransition(TARGET, targetIssue.getKey(), TARGET_STATUS_CLOSED);

		JiraIssueUpdate update = new JiraIssueUpdate();
		update.setTransition(transition);
		update.getOrCreateFields().setResolution(TARGET_RESOLUTION_DONE);
		update.getOrCreateFields().setFixVersions(Collections.singleton(TARGET_VERSION_10));
		jiraDummyService.transitionIssue(TARGET, targetIssue.getKey(), update);

		syncTask.sync();

		// then
		JiraIssue updatedSourceIssue = jiraDummyService.getIssueByKey(SOURCE, createdSourceIssue.getKey());
		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_RESOLVED.getName());
		assertThat(updatedSourceIssue.getFields().getResolution().getName()).isEqualTo(SOURCE_RESOLUTION_FIXED.getName());
		assertThat(getNames(updatedSourceIssue.getFields().getFixVersions())).containsExactly(SOURCE_VERSION_10.getName());
	}

	private JiraIssue createIssueInSource(String summary) {
		return createIssueInSource(summary, SOURCE_TYPE_BUG);
	}

	private JiraIssue createIssueInSource(String summary, JiraIssueType issueType) {
		JiraIssue sourceIssue = new JiraIssue(null, null, summary, SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(issueType);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		return jiraDummyService.createIssue(SOURCE, sourceIssue);
	}

	private JiraTransition findTransition(Context context, String issueKey, JiraIssueStatus statusToTransitionTo) {
		List<JiraTransition> transitions = jiraDummyService.getTransitions(context, issueKey).getTransitions();
		List<JiraTransition> filteredTransitions = transitions.stream()
			.filter(transition -> transition.getTo().getName().equals(statusToTransitionTo.getName()))
			.collect(Collectors.toList());
		assertThat(filteredTransitions).hasSize(1);
		return filteredTransitions.get(0);
	}

	@Test
	public void testSetTicketToInProgressInSourceWhenTargetGetsAssigned() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();
		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue currentSourceIssue = jiraDummyService.getAllIssues(TARGET).get(0);

		assertThat(currentSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_OPEN.getName());

		// when

		assertThat(jiraDummyService.getAllIssues(TARGET)).hasSize(1);
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		targetIssue.getFields().setAssignee(new JiraUser("some", "body"));

		syncTask.sync();

		// then
		JiraIssue updatedSourceIssue = jiraDummyService.getIssueByKey(SOURCE, createdSourceIssue.getKey());

		assertThat(updatedSourceIssue.getFields().getStatus().getName()).isEqualTo(SOURCE_STATUS_IN_PROGRESS.getName());
		assertThat(updatedSourceIssue.getFields().getAssignee().getKey()).isEqualTo("myself");
	}

}
