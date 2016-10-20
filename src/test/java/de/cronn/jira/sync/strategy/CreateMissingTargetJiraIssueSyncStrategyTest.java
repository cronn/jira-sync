package de.cronn.jira.sync.strategy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.link.JiraIssueWebLinker;
import de.cronn.jira.sync.mapping.DefaultDescriptionMapper;
import de.cronn.jira.sync.mapping.DefaultIssueTypeMapper;
import de.cronn.jira.sync.mapping.DefaultLabelMapper;
import de.cronn.jira.sync.mapping.DefaultPriorityMapper;
import de.cronn.jira.sync.mapping.DefaultSummaryMapper;
import de.cronn.jira.sync.mapping.DefaultUsernameReplacer;
import de.cronn.jira.sync.mapping.DefaultVersionMapper;
import de.cronn.jira.sync.mapping.IssueTypeMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.SummaryMapper;
import de.cronn.jira.sync.mapping.UsernameReplacer;
import de.cronn.jira.sync.mapping.VersionMapper;

public class CreateMissingTargetJiraIssueSyncStrategyTest extends AbstractIssueSyncStrategyTest {

	@InjectMocks
	private CreateMissingTargetJiraIssueSyncStrategy strategy;

	@InjectMocks
	@Spy
	private SummaryMapper summaryMapper = new DefaultSummaryMapper();

	@InjectMocks
	@Spy
	private DefaultDescriptionMapper descriptionMapper = new DefaultDescriptionMapper();

	@InjectMocks
	@Spy
	private IssueTypeMapper issueTypeMapper = new DefaultIssueTypeMapper();

	@InjectMocks
	@Spy
	private LabelMapper labelMapper = new DefaultLabelMapper();

	@InjectMocks
	@Spy
	private PriorityMapper priorityMapper = new DefaultPriorityMapper();

	@InjectMocks
	@Spy
	private VersionMapper versionMapper = new DefaultVersionMapper();

	@InjectMocks
	@Spy
	private JiraIssueLinker issueLinker = new JiraIssueWebLinker();

	@InjectMocks
	@Spy
	private UsernameReplacer usernameReplacer = new DefaultUsernameReplacer();

	@Test
	public void testCreateMissingTicket() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("123", "SOURCE-123", "some summary", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setDescription("the description");
		sourceIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("label1", "label2")));
		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_2));
		sourceIssue.getFields().setFixVersions(Collections.singleton(SOURCE_VERSION_2));

		JiraIssueType issueType = new JiraIssueType("4000", SOURCE_ISSUE_TYPE_NEW_FEATURE);
		sourceIssue.getFields().setIssuetype(issueType);

		ArgumentCaptor<JiraIssue> issueCaptor = ArgumentCaptor.forClass(JiraIssue.class);
		JiraIssue newIssue = new JiraIssue();
		when(jiraTarget.createIssue(issueCaptor.capture())).thenReturn(newIssue);

		// when
		SyncResult syncResult = strategy.sync(jiraSource, jiraTarget, sourceIssue, projectSync);

		// then
		assertThat(syncResult).isEqualTo(SyncResult.CREATED);
		JiraIssue createdIssue = issueCaptor.getValue();
		assertThat(createdIssue.getKey()).isNull();
		assertThat(createdIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_ISSUE_TYPE_IMPROVEMENT);
		assertThat(createdIssue.getFields().getProject()).isEqualTo(targetProject);
		assertThat(createdIssue.getFields().getSummary()).isEqualTo("SOURCE-123: some summary");
		assertThat(createdIssue.getFields().getDescription()).isEqualTo(descriptionMapper.mapSourceDescription("the description", jiraSource));
		assertThat(createdIssue.getFields().getLabels()).containsExactly("label1", "label2");
		assertThat(createdIssue.getFields().getPriority().getName()).isEqualTo(TARGET_PRIORITY_MAJOR.getName());
		assertThat(createdIssue.getFields().getVersions()).containsExactly(TARGET_VERSION_2);
		assertThat(createdIssue.getFields().getFixVersions()).containsExactly(TARGET_VERSION_2);

		verify(jiraTarget).getProjectByKey(TARGET_PROJECT_KEY);
		verify(jiraTarget).getPriorities();
		verify(jiraTarget, times(2)).getVersions(TARGET_PROJECT_KEY);
		verify(jiraTarget).createIssue(any(JiraIssue.class));
		verify(jiraSource).addRemoteLink(sourceIssue, newIssue, jiraTarget, projectSync.getRemoteLinkIconInSource());
		verify(jiraTarget).addRemoteLink(newIssue, sourceIssue, jiraSource, projectSync.getRemoteLinkIconInTarget());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testCreateMissingTicket_NoDescription_NoLabels_NoPriority() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("123", "SOURCE-123", "some summary", SOURCE_STATUS_OPEN);

		ArgumentCaptor<JiraIssue> issueCaptor = ArgumentCaptor.forClass(JiraIssue.class);
		when(jiraTarget.createIssue(issueCaptor.capture())).thenReturn(new JiraIssue());

		// when
		SyncResult syncResult = strategy.sync(jiraSource, jiraTarget, sourceIssue, projectSync);

		// then
		assertThat(syncResult).isEqualTo(SyncResult.CREATED);
		JiraIssue createdIssue = issueCaptor.getValue();
		assertThat(createdIssue.getFields().getDescription()).isEqualTo("");
		assertThat(createdIssue.getFields().getIssuetype().getName()).isEqualTo(TARGET_ISSUE_TYPE_DEFAULT);
		assertThat(createdIssue.getFields().getLabels()).isEmpty();
		assertThat(createdIssue.getFields().getPriority()).isNull();
	}

	@Test
	public void testCreateMissingTicket_UnknownPriority() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("123", "SOURCE-123", "some summary", SOURCE_STATUS_OPEN);
		sourceIssue.getFields().setPriority(new JiraPriority("12345", "Something"));

		ArgumentCaptor<JiraIssue> issueCaptor = ArgumentCaptor.forClass(JiraIssue.class);
		when(jiraTarget.createIssue(issueCaptor.capture())).thenReturn(new JiraIssue());

		// when
		SyncResult syncResult = strategy.sync(jiraSource, jiraTarget, sourceIssue, projectSync);

		// then
		assertThat(syncResult).isEqualTo(SyncResult.CREATED);
		JiraIssue createdIssue = issueCaptor.getValue();
		assertThat(createdIssue.getFields().getPriority()).isNull();
	}

}