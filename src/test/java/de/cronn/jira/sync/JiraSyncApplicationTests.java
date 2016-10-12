package de.cronn.jira.sync;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraRemoteLinkObject;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.dummy.JiraDummyService;

@RunWith(SpringRunner.class)
@SpringBootTest
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
	private static final JiraPriority TARGET_PRIORITY_CRITICAL = new JiraPriority("100", "Critical");

	private static final JiraResolution SOURCE_RESOLUTION_FIXED = new JiraResolution("1", "Fixed");
	private static final JiraResolution TARGET_RESOLUTION_DONE = new JiraResolution("100", "Done");

	@TestConfiguration
	static class Config {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		@Primary
		JiraDummyService jiraDummyService() {
			return new JiraDummyService();
		}

	}

	@Value("${de.cronn.jira.sync.source.url}")
	private URL sourceUrl;

	@Value("${de.cronn.jira.sync.target.url}")
	private URL targetUrl;

	@Autowired
	private JiraDummyService jiraSource;

	@Autowired
	private JiraDummyService jiraTarget;

	@Autowired
	private JiraSyncTask syncTask;

	@Autowired
	private JiraSyncConfig syncConfig;

	@Before
	public void setUp() throws Exception {
		JiraDummyService.reset();

		assertNotSame(jiraSource, jiraTarget);

		jiraSource.setUrl(sourceUrl);
		jiraTarget.setUrl(targetUrl);

		jiraSource.addProject(SOURCE_PROJECT);
		jiraTarget.addProject(TARGET_PROJECT);

		jiraSource.addPriority(SOURCE_PRIORITY_HIGH);
		jiraTarget.addPriority(TARGET_PRIORITY_CRITICAL);

		jiraSource.addResolution(SOURCE_RESOLUTION_FIXED);

		jiraSource.addTransition(new JiraTransition("1", "Set resolved", SOURCE_STATUS_RESOLVED));
		jiraSource.addTransition(new JiraTransition("2", "Set in progress", SOURCE_STATUS_IN_PROGRESS));

		jiraTarget.setDefaultStatus(TARGET_STATUS_OPEN);

		TARGET_PROJECT.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT, TARGET_TYPE_TASK));

		jiraSource.expectLoginRequest("jira-sync", "secret in source");
		jiraTarget.expectLoginRequest("jira-sync", "secret in target");
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
	public void testEmptySync() throws Exception {
		// when
		syncTask.sync();

		// then
		assertThat(jiraTarget.getAllIssues(), empty());
	}

	@Test
	public void testCreateTicketInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		jiraSource.createIssue(sourceIssue);

		// when
		syncTask.sync();

		// then
		assertThat(jiraTarget.getAllIssues(), hasSize(1));
		JiraIssue targetIssue = jiraTarget.getAllIssues().get(0);
		assertThat(targetIssue.getFields().getSummary(), is("PROJECT_ONE-1: My first bug"));
		assertThat(targetIssue.getFields().getIssuetype(), is(TARGET_TYPE_BUG));
		assertThat(targetIssue.getFields().getPriority(), is(TARGET_PRIORITY_CRITICAL));

		List<JiraRemoteLink> remoteLinksInTarget = jiraTarget.getRemoteLinks(targetIssue);
		List<JiraRemoteLink> remoteLinksInSource = jiraSource.getRemoteLinks(sourceIssue);
		assertThat(remoteLinksInTarget, hasSize(1));
		assertThat(remoteLinksInSource, hasSize(1));

		JiraRemoteLinkObject firstRemoteLinkInSource = remoteLinksInSource.get(0).getObject();
		assertThat(firstRemoteLinkInSource.getUrl(), is(new URL("https://jira-target/browse/PRJ_ONE-1")));
		assertThat(firstRemoteLinkInSource.getIcon().getUrl16x16(), is(new URL("https://jira-source/favicon.ico")));

		JiraRemoteLinkObject firstRemoteLinkInTarget = remoteLinksInTarget.get(0).getObject();
		assertThat(firstRemoteLinkInTarget.getUrl(), is(new URL("https://jira-source/browse/PROJECT_ONE-1")));
		assertThat(firstRemoteLinkInTarget.getIcon().getUrl16x16(), is(new URL("https://jira-target/favicon.ico")));
	}

	@Test
	public void testCreateTicketInTarget_WithFallbackType() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "some issue", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_UNKNOWN);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		jiraSource.createIssue(sourceIssue);

		// when
		syncTask.sync();

		// then
		assertThat(jiraTarget.getAllIssues(), hasSize(1));
		JiraIssue targetIssue = jiraTarget.getAllIssues().get(0);
		assertThat(targetIssue.getFields().getIssuetype(), is(TARGET_TYPE_TASK));
	}

	@Test
	public void testUpdateTicketInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		jiraSource.createIssue(sourceIssue);

		syncTask.sync();

		assertThat(jiraTarget.getAllIssues(), hasSize(1));
		JiraIssue targetIssue = jiraTarget.getAllIssues().get(0);
		assertThat(targetIssue.getFields().getDescription(), is(""));

		// when
		sourceIssue.getFields().setDescription("changed description");

		syncTask.sync();

		// then
		assertThat(targetIssue.getFields().getDescription(), is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged description\n{panel}"));
	}

	@Test
	public void testSetTicketToResolvedInSourceWhenTargetTicketIsClosed() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		jiraSource.createIssue(sourceIssue);

		syncTask.sync();

		assertThat(jiraTarget.getAllIssues(), hasSize(1));
		JiraIssue targetIssue = jiraTarget.getAllIssues().get(0);

		// when
		targetIssue.getFields().setStatus(TARGET_STATUS_CLOSED);
		targetIssue.getFields().setResolution(TARGET_RESOLUTION_DONE);

		syncTask.sync();

		// then
		assertThat(sourceIssue.getFields().getStatus(), sameInstance(SOURCE_STATUS_RESOLVED));
		assertThat(sourceIssue.getFields().getResolution(), sameInstance(SOURCE_RESOLUTION_FIXED));
	}

	@Test
	public void testSetTicketToInProgressInSourceWhenTargetGetsAssigned() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue(null, null, "My first bug", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setProject(SOURCE_PROJECT);
		sourceIssue.getFields().setIssuetype(SOURCE_TYPE_BUG);
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		jiraSource.createIssue(sourceIssue);

		syncTask.sync();
		syncTask.sync();

		assertThat(jiraSource.getAllIssues(), hasSize(1));
		JiraIssue currentSourceIssue = jiraSource.getAllIssues().get(0);

		assertThat(currentSourceIssue.getFields().getStatus(), sameInstance(SOURCE_STATUS_OPEN));

		// when

		assertThat(jiraTarget.getAllIssues(), hasSize(1));
		JiraIssue targetIssue = jiraTarget.getAllIssues().get(0);
		targetIssue.getFields().setAssignee(new JiraUser("some", "body"));

		syncTask.sync();

		assertThat(currentSourceIssue.getFields().getStatus(), sameInstance(SOURCE_STATUS_IN_PROGRESS));
		assertThat(currentSourceIssue.getFields().getAssignee().getKey(), is("myself"));
	}

}
