package de.cronn.jira.sync.dummy;

import static de.cronn.jira.sync.dummy.JiraDummyService.Context.TARGET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import de.cronn.jira.sync.TestClock;
import de.cronn.jira.sync.domain.JiraFieldsUpdate;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueHistoryItem;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.domain.WellKnownJiraField;

public class JiraDummyServiceTest {
	private static final JiraVersion JIRA_VERSION_2_0 = new JiraVersion("2", "2.0");
	private static final JiraVersion JIRA_VERSION_1_0 = new JiraVersion("1", "1.0");
	private static final JiraResolution RESOLUTION_WONT_DO = new JiraResolution("2", "Won't Do");
	private static final JiraResolution RESOLUTION_DONE = new JiraResolution("1", "Done");
	private static final JiraPriority PRIORITY_DEFAULT = new JiraPriority("1", "Default");
	private static final JiraIssueStatus STATUS_OPEN = new JiraIssueStatus("1", "Open");
	private static final JiraIssueStatus STATUS_IN_PROGRESS = new JiraIssueStatus("2", "In Progress");
	private static final JiraProject PROJECT = new JiraProject("1", "TST");

	private TestClock clock;

	private JiraDummyService jiraDummyService;

	@Before
	public void setup() {
		clock = new TestClock();
		jiraDummyService = createAndSetUpJiraService(clock);
	}

	@Test
	public void testGetIssueByKey() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		MockMvc mockMvc = standaloneSetup(jiraDummyService).build();

		mockMvc.perform(get("/TARGET/rest/api/2/issue/" + issue.getKey()))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.key", is(issue.getKey())))
			.andExpect(MockMvcResultMatchers.jsonPath("$.changelog", is(IsNull.nullValue())));
	}

	@Test
	public void testGetIssueByKey_wichChangelog() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate()
			.withTransition(new JiraTransition("2", "In Progress", STATUS_IN_PROGRESS));
		jiraDummyService.transitionIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		MockMvc mockMvc = standaloneSetup(jiraDummyService).build();

		mockMvc.perform(get("/TARGET/rest/api/2/issue/" + issue.getKey() + "?expand=changelog"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.key", is(issue.getKey())))
			.andExpect(MockMvcResultMatchers.jsonPath("$.changelog", is(IsNull.notNullValue())));
	}


	@Test
	public void testTransitionIssue() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate()
			.withTransition(new JiraTransition("2", "In Progress", STATUS_IN_PROGRESS));

		jiraDummyService.transitionIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getStatus().getName()).isEqualTo(STATUS_IN_PROGRESS.getName());
		assertLastHistoryEntryIs(issue, WellKnownJiraField.STATUS.getFieldName(), STATUS_OPEN.getName(), STATUS_IN_PROGRESS.getName());
	}

	@Test
	public void testUpdateIssue_emptyUpdate() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate();

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertLastHistoryEntryIsEmpty(issue);
	}

	@Test
	public void testUpdateIssue_description() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withDescription("Some description"));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getDescription()).isEqualTo("Some description");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.DESCRIPTION.getFieldName(), null, "Some description");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withDescription("New description"));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getDescription()).isEqualTo("New description");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.DESCRIPTION.getFieldName(), "Some description", "New description");
	}

	@Test
	public void testUpdateIssue_resolution() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withResolution(RESOLUTION_DONE));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getResolution().getName()).isEqualTo("Done");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.RESOLUTION.getFieldName(), null, "Done");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withResolution(RESOLUTION_WONT_DO));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getResolution().getName()).isEqualTo("Won't Do");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.RESOLUTION.getFieldName(), "Done", "Won't Do");
	}

	@Test
	public void testUpdateIssue_assignee() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withAssignee(new JiraUser("johnny", "1")));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getAssignee().getName()).isEqualTo("johnny");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.ASSIGNEE.getFieldName(), null, "johnny");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withAssignee(new JiraUser("tommy", "2")));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), new String[] {"changelog"});

		assertThat(issue.getFields().getAssignee().getName()).isEqualTo("tommy");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.ASSIGNEE.getFieldName(), "johnny", "tommy");
	}

	private void assertLastHistoryEntryIsEmpty(JiraIssue issue) {
		JiraIssueHistoryEntry lastHistoryEntry = getLastHistoryEntry(issue);

		assertThat(lastHistoryEntry.getCreated()).isEqualTo(ZonedDateTime.now(clock));
		assertThat(lastHistoryEntry.getItems()).isEmpty();
	}

	private void assertLastHistoryEntryIs(JiraIssue issue, String field, String fromString, String toString) {
		JiraIssueHistoryEntry lastHistoryEntry = getLastHistoryEntry(issue);

		assertThat(lastHistoryEntry.getCreated()).isEqualTo(ZonedDateTime.now(clock));
		assertThat(lastHistoryEntry.getItems())
			.containsExactly(new JiraIssueHistoryItem(field).withFromString(fromString).withToString(toString));
	}

	private JiraIssueHistoryEntry getLastHistoryEntry(JiraIssue issue) {
		List<JiraIssueHistoryEntry> history = issue.getChangelog().getHistory();
		JiraIssueHistoryEntry lastHistoryEntry = history.get(history.size() - 1);
		return lastHistoryEntry;
	}

	private JiraIssue createJiraIssue(JiraDummyService jiraDummyService) {
		JiraIssue issue = new JiraIssue()
			.withFields(new JiraIssueFields().withProject(PROJECT).withStatus(STATUS_OPEN).withPriority(PRIORITY_DEFAULT));

		ResponseEntity<Object> response = jiraDummyService.createIssue(TARGET, issue);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return issue;
	}

	private static JiraDummyService createAndSetUpJiraService(Clock clock) {
		JiraDummyService jiraDummyService = new JiraDummyService();
		jiraDummyService.setClock(clock);
		jiraDummyService.addProject(TARGET, PROJECT);
		jiraDummyService.addPriority(TARGET, PRIORITY_DEFAULT);
		jiraDummyService.addResolution(TARGET, RESOLUTION_DONE);
		jiraDummyService.addResolution(TARGET, RESOLUTION_WONT_DO);
		jiraDummyService.addVersion(TARGET, JIRA_VERSION_1_0);
		jiraDummyService.addVersion(TARGET, JIRA_VERSION_2_0);
		jiraDummyService.addTransition(TARGET, new JiraTransition("2", "Set in progress", STATUS_IN_PROGRESS));
		return jiraDummyService;
	}

}
