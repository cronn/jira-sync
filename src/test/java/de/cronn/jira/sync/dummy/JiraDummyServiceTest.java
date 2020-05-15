package de.cronn.jira.sync.dummy;

import static de.cronn.jira.sync.config.Context.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import de.cronn.jira.sync.TestClock;
import de.cronn.jira.sync.domain.JiraComponent;
import de.cronn.jira.sync.domain.JiraFieldsUpdate;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueHistoryItem;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraNamedResource;
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
	private static final JiraComponent JIRA_COMPONENT_BACKEND = new JiraComponent("2", "Backend");
	private static final JiraComponent JIRA_COMPONENT_FRONTEND = new JiraComponent("1", "Frontend");
	private static final JiraResolution RESOLUTION_WONT_DO = new JiraResolution("2", "Won't Do");
	private static final JiraResolution RESOLUTION_DONE = new JiraResolution("1", "Done");
	private static final JiraPriority PRIORITY_DEFAULT = new JiraPriority("1", "Default");
	private static final JiraIssueStatus STATUS_OPEN = new JiraIssueStatus("1", "Open");
	private static final JiraIssueStatus STATUS_IN_PROGRESS = new JiraIssueStatus("2", "In Progress");
	private static final JiraProject PROJECT = new JiraProject("1", "TST");
	private static final String CHANGELOG = JiraDummyService.CHANGELOG;

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
	public void testGetIssueByKey_withChangelog() throws Exception {
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
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getStatus()).isEqualTo(STATUS_IN_PROGRESS);
		assertLastHistoryEntryIs(issue, WellKnownJiraField.STATUS, STATUS_OPEN.getName(), STATUS_IN_PROGRESS.getName());
	}

	@Test
	public void testUpdateIssue_emptyUpdate() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate();

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertLastHistoryEntryIsEmpty(issue);
	}

	@Test
	public void testUpdateIssue_description() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withDescription("Some description"));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getDescription()).isEqualTo("Some description");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.DESCRIPTION, null, "Some description");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withDescription("New description"));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getDescription()).isEqualTo("New description");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.DESCRIPTION, "Some description", "New description");
	}

	@Test
	public void testUpdateIssue_resolution() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withResolution(RESOLUTION_DONE));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getResolution().getName()).isEqualTo("Done");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.RESOLUTION, null, "Done");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withResolution(RESOLUTION_WONT_DO));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getResolution().getName()).isEqualTo("Won't Do");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.RESOLUTION, "Done", "Won't Do");
	}

	@Test
	public void testUpdateIssue_assignee() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);
		JiraIssueUpdate jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withAssignee(new JiraUser("johnny", "1")));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getAssignee().getName()).isEqualTo("johnny");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.ASSIGNEE, null, "johnny");

		jiraIssueUpdate = new JiraIssueUpdate().withFields(new JiraFieldsUpdate().withAssignee(new JiraUser("tommy", "2")));

		jiraDummyService.updateIssue(TARGET, issue.getKey(), jiraIssueUpdate);
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getAssignee().getName()).isEqualTo("tommy");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.ASSIGNEE, "johnny", "tommy");
	}

	@Test
	public void testUpdateIssue_fixVersions() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createFixVersionUpdate("1.0"));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getFixVersions()).extracting(JiraVersion::getName).containsExactly("1.0");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.FIX_VERSIONS, null, "1.0");

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createFixVersionUpdate("1.0", "2.0"));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getFixVersions()).extracting(JiraVersion::getName).containsExactly("1.0", "2.0");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.FIX_VERSIONS, null, "2.0");
	}

	@Test
	public void testUpdateIssue_fixVersions_multipleVersionChanges() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createFixVersionUpdate("1.0", "2.0"));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getFixVersions()).extracting(JiraVersion::getName).containsExactly("1.0", "2.0");
		assertThat(getLastHistoryEntry(issue).getItems()).extracting(JiraIssueHistoryItem::getFromString).containsExactly(null, null);
		assertThat(getLastHistoryEntry(issue).getItems()).extracting(JiraIssueHistoryItem::getToString).containsExactly("1.0", "2.0");

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createFixVersionUpdate());
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getFixVersions()).isEmpty();
		assertThat(getLastHistoryEntry(issue).getItems()).extracting(JiraIssueHistoryItem::getToString).containsExactly(null, null);
		assertThat(getLastHistoryEntry(issue).getItems()).extracting(JiraIssueHistoryItem::getFromString).containsExactly("1.0", "2.0");
	}

	@Test
	public void testUpdateIssue_versions() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createVersionUpdate("1.0"));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getVersions()).extracting(JiraVersion::getName).containsExactly("1.0");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.VERSIONS, null, "1.0");

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createVersionUpdate("1.0", "2.0"));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getVersions()).extracting(JiraVersion::getName).containsExactly("1.0", "2.0");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.VERSIONS, null, "2.0");
	}

	private static JiraIssueUpdate createFixVersionUpdate(String... newVersions) {
		return new JiraIssueUpdate()
			.withFields(new JiraFieldsUpdate()
				.withFixVersions(versions(newVersions)));
	}

	private static JiraIssueUpdate createVersionUpdate(String... newVersions) {
		return new JiraIssueUpdate()
			.withFields(new JiraFieldsUpdate()
				.withVersions(versions(newVersions)));
	}

	private static Set<JiraVersion> versions(String... versionNames) {
		Set<JiraVersion> versions = new LinkedHashSet<>();
		for (String versionName : versionNames) {
			String id = String.valueOf(versions.size() + 1);
			versions.add(new JiraVersion(id, versionName));
		}
		return versions;
	}

	@Test
	public void testUpdateIssue_components() throws Exception {
		JiraIssue issue = createJiraIssue(jiraDummyService);

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createComponentUpdate(JIRA_COMPONENT_BACKEND));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getComponents()).extracting(JiraComponent::getName).containsExactly("Backend");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.COMPONENTS, null, "Backend");

		jiraDummyService.updateIssue(TARGET, issue.getKey(), createComponentUpdate(JIRA_COMPONENT_BACKEND, JIRA_COMPONENT_FRONTEND));
		issue = jiraDummyService.getIssueByKey(TARGET, issue.getKey(), CHANGELOG);

		assertThat(issue.getFields().getComponents()).extracting(JiraComponent::getName).containsExactly("Backend", "Frontend");
		assertLastHistoryEntryIs(issue, WellKnownJiraField.COMPONENTS, null, "Frontend");
	}

	private static JiraIssueUpdate createComponentUpdate(JiraComponent... newComponents) {
		return new JiraIssueUpdate()
			.withFields(new JiraFieldsUpdate()
				.withComponents(new LinkedHashSet<>(Arrays.asList(newComponents))));
	}

	private void assertLastHistoryEntryIsEmpty(JiraIssue issue) {
		JiraIssueHistoryEntry lastHistoryEntry = getLastHistoryEntry(issue);

		assertThat(lastHistoryEntry.getCreated()).isEqualTo(ZonedDateTime.now(clock));
		assertThat(lastHistoryEntry.getItems()).isEmpty();
	}

	private void assertLastHistoryEntryIs(JiraIssue issue, WellKnownJiraField field, String fromString, String toString) {
		JiraIssueHistoryEntry lastHistoryEntry = getLastHistoryEntry(issue);

		assertThat(lastHistoryEntry.getCreated()).isEqualTo(ZonedDateTime.now(clock));
		assertThat(lastHistoryEntry.getItems())
			.containsExactly(new JiraIssueHistoryItem(field).withFromString(fromString).withToString(toString));
	}

	private JiraIssueHistoryEntry getLastHistoryEntry(JiraIssue issue) {
		List<JiraIssueHistoryEntry> histories = issue.getChangelog().getHistories();
		JiraIssueHistoryEntry lastHistoryEntry = histories.get(histories.size() - 1);
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
		jiraDummyService.addComponent(TARGET, JIRA_COMPONENT_FRONTEND);
		jiraDummyService.addComponent(TARGET, JIRA_COMPONENT_BACKEND);
		jiraDummyService.addTransition(TARGET, new JiraTransition("2", "Set in progress", STATUS_IN_PROGRESS));
		return jiraDummyService;
	}

}
