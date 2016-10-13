package de.cronn.jira.sync;

import static de.cronn.jira.sync.dummy.JiraDummyService.Context.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URL;
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
	private static final JiraVersion SOURCE_VERSION_UNMAPPED = new JiraVersion("99", "Unmapped version");

	private static final JiraVersion TARGET_VERSION_10 = new JiraVersion("100", "10");
	private static final JiraVersion TARGET_VERSION_11 = new JiraVersion("101", "11");

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
	public void setUp() throws Exception {

		String commonBaseUrl = "http://localhost:" + port + "/";
		sourceBaseUrl = commonBaseUrl + Context.SOURCE + "/";
		targetBaseUrl = commonBaseUrl + Context.TARGET + "/";

		syncConfig.getSource().setUrl(new URL(sourceBaseUrl));
		syncConfig.getTarget().setUrl(new URL(targetBaseUrl));

		jiraDummyService.reset();

		jiraDummyService.setBaseUrl(SOURCE, sourceBaseUrl);
		jiraDummyService.setBaseUrl(TARGET, targetBaseUrl);

		jiraDummyService.addProject(SOURCE, SOURCE_PROJECT);
		jiraDummyService.addProject(TARGET, TARGET_PROJECT);

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
		jiraDummyService.addVersion(SOURCE, SOURCE_VERSION_UNMAPPED);

		jiraDummyService.addVersion(TARGET, TARGET_VERSION_10);
		jiraDummyService.addVersion(TARGET, TARGET_VERSION_11);

		jiraDummyService.setDefaultStatus(TARGET, TARGET_STATUS_OPEN);

		TARGET_PROJECT.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));

		jiraDummyService.expectLoginRequest(SOURCE, "jira-sync", "secret in source");
		jiraDummyService.expectLoginRequest(TARGET, "jira-sync", "secret in target");
	}

	@Test
	public void testConfiguration() throws Exception {
		Map<String, String> resolutionMapping = syncConfig.getResolutionMapping();
		assertThat(resolutionMapping.keySet(), containsInAnyOrder("Fixed", "Duplicate", "Incomplete", "Won't Fix", "Won't Do", "Cannot Reproduce", "Done"));

		assertThat(resolutionMapping.get("Fixed"), is("Fixed"));
		assertThat(resolutionMapping.get("Done"), is("Fixed"));
		assertThat(resolutionMapping.get("Cannot Reproduce"), is("Cannot Reproduce"));
		assertThat(resolutionMapping.get("Won't Fix"), is("Won't Fix"));
	}

	@Test
	public void testResolutionsAreCached() throws Exception {
		assertNotSame(jiraSource, jiraTarget);

		try {
			jiraSource.login(jiraSyncConfig.getSource());
			jiraTarget.login(jiraSyncConfig.getTarget());

			List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
			List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();
			assertSame(sourceResolutions1, sourceResolutions2);

			List<JiraResolution> targetResolutions1 = jiraTarget.getResolutions();
			List<JiraResolution> targetResolutions2 = jiraTarget.getResolutions();
			assertSame(targetResolutions1, targetResolutions2);

			assertNotSame(sourceResolutions1, targetResolutions1);
		} finally {
			jiraSource.logout();
			jiraTarget.logout();
		}
	}

	@Test
	public void testResolutionsAndPrioritiesAreCached() throws Exception {
		try {
			jiraSource.login(jiraSyncConfig.getSource());

			List<JiraResolution> sourceResolutions1 = jiraSource.getResolutions();
			List<JiraPriority> sourcePriorities1 = jiraSource.getPriorities();
			List<JiraResolution> sourceResolutions2 = jiraSource.getResolutions();
			List<JiraPriority> sourcePriorities2 = jiraSource.getPriorities();
			assertSame(sourceResolutions1, sourceResolutions2);
			assertSame(sourcePriorities1, sourcePriorities2);
			assertNotSame(sourcePriorities1, sourceResolutions1);
		} finally {
			jiraSource.logout();
		}
	}

	@Test
	public void testCacheGetsEvictedOnLogout() throws Exception {
		try {
			jiraSource.login(jiraSyncConfig.getSource());

			List<JiraResolution> resolutions1 = jiraSource.getResolutions();
			List<JiraResolution> resolutions2 = jiraSource.getResolutions();
			assertSame(resolutions1, resolutions2);

			jiraSource.logout();
			jiraSource.login(jiraSyncConfig.getSource());

			List<JiraResolution> resolutions3 = jiraSource.getResolutions();
			List<JiraResolution> resolutions4 = jiraSource.getResolutions();
			assertNotSame(resolutions1, resolutions3);
			assertSame(resolutions3, resolutions4);

		} finally {
			jiraSource.logout();
		}
	}

	@Test
	public void testEmptySync() throws Exception {
		// when
		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET), empty());
	}

	@Test
	public void testCreateTicketInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("label1", "label2")));
		sourceIssue.getFields().setVersions(new LinkedHashSet<>(Arrays.asList(SOURCE_VERSION_10, SOURCE_VERSION_11)));
		sourceIssue.getFields().setFixVersions(Collections.singleton(SOURCE_VERSION_11));
		jiraDummyService.createIssue(SOURCE, sourceIssue);

		// when
		syncTask.sync();

		// then
		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getSummary(), is("PROJECT_ONE-1: My first bug"));
		assertThat(targetIssue.getFields().getIssuetype().getName(), is(TARGET_TYPE_BUG.getName()));
		assertThat(targetIssue.getFields().getPriority().getName(), is(TARGET_PRIORITY_CRITICAL.getName()));
		assertThat(targetIssue.getFields().getLabels(), contains("label1", "label2"));
		assertThat(getNames(targetIssue.getFields().getVersions()), containsInAnyOrder("10", "11"));
		assertThat(getNames(targetIssue.getFields().getFixVersions()), contains("11"));

		List<JiraRemoteLink> remoteLinksInTarget = jiraDummyService.getRemoteLinks(TARGET, targetIssue);
		List<JiraRemoteLink> remoteLinksInSource = jiraDummyService.getRemoteLinks(SOURCE, sourceIssue);
		assertThat(remoteLinksInTarget, hasSize(1));
		assertThat(remoteLinksInSource, hasSize(1));

		JiraRemoteLinkObject firstRemoteLinkInSource = remoteLinksInSource.get(0).getObject();
		assertThat(firstRemoteLinkInSource.getUrl(), is(new URL(targetBaseUrl + "/browse/PRJ_ONE-1")));
		assertThat(firstRemoteLinkInSource.getIcon().getUrl16x16(), is(new URL("https://jira-source/favicon.ico")));

		JiraRemoteLinkObject firstRemoteLinkInTarget = remoteLinksInTarget.get(0).getObject();
		assertThat(firstRemoteLinkInTarget.getUrl(), is(new URL(sourceBaseUrl + "/browse/PROJECT_ONE-1")));
		assertThat(firstRemoteLinkInTarget.getIcon().getUrl16x16(), is(new URL("https://jira-target/favicon.ico")));
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
		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getIssuetype().getName(), is(TARGET_TYPE_BUG.getName()));
		assertNull(targetIssue.getFields().getPriority());
		assertThat(targetIssue.getFields().getVersions(), empty());
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
		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getIssuetype().getName(), is(TARGET_TYPE_TASK.getName()));
	}

	@Test
	public void testUpdateTicketInTarget() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		assertThat(targetIssue.getFields().getDescription(), is(""));

		// when
		JiraIssueUpdate update = new JiraIssueUpdate();
		update.getOrCreateFields().setDescription("changed description");
		jiraDummyService.updateIssue(SOURCE, createdSourceIssue.getKey(), update);

		syncTask.sync();

		// then
		assertThat(targetIssue.getFields().getDescription(), is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged description\n{panel}"));
	}

	@Test
	public void testSetTicketToResolvedInSourceWhenTargetTicketIsClosed() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
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
		assertThat(updatedSourceIssue.getFields().getStatus().getName(), is(SOURCE_STATUS_RESOLVED.getName()));
		assertThat(updatedSourceIssue.getFields().getResolution().getName(), is(SOURCE_RESOLUTION_FIXED.getName()));
		assertThat(getNames(updatedSourceIssue.getFields().getFixVersions()), contains(SOURCE_VERSION_10.getName()));
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
 		assertThat(filteredTransitions, hasSize(1));
		return filteredTransitions.get(0);
	}

	@Test
	public void testSetTicketToInProgressInSourceWhenTargetGetsAssigned() throws Exception {
		// given
		JiraIssue createdSourceIssue = createIssueInSource("My first bug");

		syncTask.sync();
		syncTask.sync();

		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue currentSourceIssue = jiraDummyService.getAllIssues(TARGET).get(0);

		assertThat(currentSourceIssue.getFields().getStatus().getName(), is(SOURCE_STATUS_OPEN.getName()));

		// when

		assertThat(jiraDummyService.getAllIssues(TARGET), hasSize(1));
		JiraIssue targetIssue = jiraDummyService.getAllIssues(TARGET).get(0);
		targetIssue.getFields().setAssignee(new JiraUser("some", "body"));

		syncTask.sync();

		// then
		JiraIssue updatedSourceIssue = jiraDummyService.getIssueByKey(SOURCE, createdSourceIssue.getKey());

		assertThat(updatedSourceIssue.getFields().getStatus().getName(), is(SOURCE_STATUS_IN_PROGRESS.getName()));
		assertThat(updatedSourceIssue.getFields().getAssignee().getKey(), is("myself"));
	}

}
