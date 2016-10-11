package de.cronn.jira.sync.strategy;

import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.config.StatusTransitionConfig;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraTransition;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractIssueSyncStrategyTest {

	protected static final String TARGET_PROJECT_KEY = "TARGET";
	protected static final String SOURCE_PROJECT_KEY = "SOURCE";
	protected static final String ISSUE_TYPE_TASK = "Task";
	protected static final String TARGET_ISSUE_TYPE_IMPROVEMENT = "Improvement";
	protected static final String SOURCE_ISSUE_TYPE_NEW_FEATURE = "New Feature";
	protected static final String TARGET_ISSUE_TYPE_DEFAULT = ISSUE_TYPE_TASK;

	protected static final JiraIssueStatus SOURCE_STATUS_OPEN = new JiraIssueStatus("10", "Open");
	protected static final JiraIssueStatus SOURCE_STATUS_RESOLVED = new JiraIssueStatus("20", "Resolved");

	protected static final JiraIssueStatus TARGET_STATUS_OPEN = new JiraIssueStatus("300", "Open");
	protected static final JiraIssueStatus TARGET_STATUS_IN_PROGRESS = new JiraIssueStatus("301", "In Progress");
	protected static final JiraIssueStatus TARGET_STATUS_CLOSED = new JiraIssueStatus("303", "Closed");

	protected static final JiraTransition SOURCE_TRANSITION_RESOLVE = new JiraTransition("1", "resolve it", SOURCE_STATUS_RESOLVED);

	protected static final JiraPriority SOURCE_PRIORITY_HIGH = new JiraPriority("10000", "High");
	protected static final JiraPriority SOURCE_PRIORITY_LOW = new JiraPriority("20000", "Low");

	protected static final JiraPriority TARGET_PRIORITY_MAJOR = new JiraPriority("800", "Major");
	protected static final JiraPriority TARGET_PRIORITY_MINOR =  new JiraPriority("700", "Minor");

	protected static final JiraVersion SOURCE_VERSION_1 = new JiraVersion("1", "1.0");
	protected static final JiraVersion SOURCE_VERSION_2 = new JiraVersion("2", "2.0");

	protected static final JiraVersion TARGET_VERSION_1 = new JiraVersion("100", "Release 1");
	protected static final JiraVersion TARGET_VERSION_2 = new JiraVersion("200", "Release 2");

	@Mock
	protected JiraService jiraSource;

	@Mock
	protected JiraService jiraTarget;

	@Spy
	private JiraSyncConfig jiraSyncConfig = new JiraSyncConfig();

	protected JiraProject targetProject;

	protected JiraProjectSync projectSync;

	@Before
	public void setUpTargetProject() throws Exception {
		targetProject = new JiraProject("1", TARGET_PROJECT_KEY);
		targetProject.setIssueTypes(Arrays.asList(
			new JiraIssueType("10", ISSUE_TYPE_TASK),
			new JiraIssueType("20", TARGET_ISSUE_TYPE_IMPROVEMENT)
		));

		when(jiraTarget.getProjectByKey(TARGET_PROJECT_KEY)).thenReturn(targetProject);

		projectSync = createProjectSyncConfiguration();
	}

	@Before
	public void setUpGetPriorities() {
		when(jiraTarget.getPriorities()).thenReturn(Arrays.asList(TARGET_PRIORITY_MAJOR, TARGET_PRIORITY_MINOR));
	}

	@Before
	public void setUpGetVersions() {
		when(jiraTarget.getVersions(TARGET_PROJECT_KEY)).thenReturn(Arrays.asList(TARGET_VERSION_1, TARGET_VERSION_2));
	}

	private JiraProjectSync createProjectSyncConfiguration() throws Exception {
		JiraProjectSync projectSync = new JiraProjectSync();
		projectSync.setSourceProject(SOURCE_PROJECT_KEY);
		projectSync.setTargetProject(TARGET_PROJECT_KEY);
		projectSync.setRemoteLinkIconInSource(new URL("https://remote-link-icon-in-source"));
		projectSync.setRemoteLinkIconInTarget(new URL("https://remote-link-icon-in-target"));
		projectSync.setTargetIssueTypeFallback(TARGET_ISSUE_TYPE_DEFAULT);

		Map<String, String> versionMapping = new LinkedHashMap<>();
		versionMapping.put(SOURCE_VERSION_1.getName(), TARGET_VERSION_1.getName());
		versionMapping.put(SOURCE_VERSION_2.getName(), TARGET_VERSION_2.getName());
		projectSync.setVersionMapping(versionMapping);

		Map<String, String> priorityMapping = new LinkedHashMap<>();
		priorityMapping.put(SOURCE_PRIORITY_HIGH.getName(), TARGET_PRIORITY_MAJOR.getName());
		priorityMapping.put(SOURCE_PRIORITY_LOW.getName(), TARGET_PRIORITY_MINOR.getName());
		jiraSyncConfig.setPriorityMapping(priorityMapping);

		Map<String, String> issueTypeMapping = new LinkedHashMap<>();
		issueTypeMapping.put(ISSUE_TYPE_TASK, ISSUE_TYPE_TASK);
		issueTypeMapping.put(SOURCE_ISSUE_TYPE_NEW_FEATURE, TARGET_ISSUE_TYPE_IMPROVEMENT);
		jiraSyncConfig.setIssueTypeMapping(issueTypeMapping);

		projectSync.setStatusTransitions(Collections.singletonList(
			new StatusTransitionConfig(
				Collections.singletonList(SOURCE_STATUS_OPEN.getName()),
				Collections.singletonList(TARGET_STATUS_CLOSED.getName()),
				SOURCE_STATUS_RESOLVED.getName()
			)
		));

		return projectSync;
	}

}
