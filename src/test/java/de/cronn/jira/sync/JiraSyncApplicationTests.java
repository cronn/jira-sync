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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraRemoteLink;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JiraSyncApplicationTests {

	private static final JiraProject SOURCE_PROJECT = new JiraProject("1", "PROJECT_ONE");
	private static final JiraProject TARGET_PROJECT = new JiraProject("100", "PRJ_ONE");

	private static final JiraIssueStatus SOURCE_STATUS_OPEN = new JiraIssueStatus("1", "Open");
	private static final JiraIssueStatus TARGET_STATUS_OPEN = new JiraIssueStatus("100", "Open");

	private static final JiraIssueType SOURCE_TYPE_BUG = new JiraIssueType("1", "Bug");
	private static final JiraIssueType TARGET_TYPE_BUG = new JiraIssueType("100", "Bug");
	private static final JiraIssueType TARGET_TYPE_IMPROVEMENT = new JiraIssueType("101", "Improvement");

	private static final JiraPriority SOURCE_PRIORITY_HIGH = new JiraPriority("1", "High");
	private static final JiraPriority TARGET_PRIORITY_CRITICAL = new JiraPriority("100", "Critical");

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

		jiraSource.setUrl(new URL("https://jira-source"));
		jiraTarget.setUrl(new URL("https://jira-target"));

		jiraSource.addProject(SOURCE_PROJECT);
		jiraTarget.addProject(TARGET_PROJECT);

		jiraSource.addPriority(SOURCE_PRIORITY_HIGH);
		jiraTarget.addPriority(TARGET_PRIORITY_CRITICAL);

		jiraTarget.setDefaultStatus(TARGET_STATUS_OPEN);

		TARGET_PROJECT.setIssueTypes(Arrays.asList(TARGET_TYPE_BUG, TARGET_TYPE_IMPROVEMENT));

		jiraSource.expectLoginRequest("jira-sync", "secret in source");
		jiraTarget.expectLoginRequest("jira-sync", "secret in target");
	}

	@Test
	public void testConfiguration() throws Exception {
		Map<String, String> resolutionMapping = syncConfig.getResolutionMapping();
		assertThat(resolutionMapping.keySet(), containsInAnyOrder("Fixed", "Duplicate", "Incomplete", "Won't Fix", "Won't Do", "Cannot Reproduce", "Done"));
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

		assertThat(remoteLinksInTarget.get(0).getObject().getUrl(), is(new URL("https://jira-source/browse/PROJECT_ONE-1")));
		assertThat(remoteLinksInSource.get(0).getObject().getUrl(), is(new URL("https://jira-target/browse/PRJ_ONE-1")));
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

}
