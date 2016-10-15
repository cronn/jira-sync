package de.cronn.jira.sync.strategy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.mapping.DefaultDescriptionMapper;
import de.cronn.jira.sync.mapping.DefaultIssueTypeMapper;
import de.cronn.jira.sync.mapping.DefaultLabelMapper;
import de.cronn.jira.sync.mapping.DefaultPriorityMapper;
import de.cronn.jira.sync.mapping.DefaultResolutionMapper;
import de.cronn.jira.sync.mapping.DefaultVersionMapper;
import de.cronn.jira.sync.mapping.IssueTypeMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.ResolutionMapper;
import de.cronn.jira.sync.mapping.VersionMapper;

public class UpdateExistingTargetJiraIssueSyncStrategyTest extends AbstractIssueSyncStrategyTest {

	@Mock
	private JiraIssueLinker jiraIssueLinker;

	@InjectMocks
	private UpdateExistingTargetJiraIssueSyncStrategy strategy;

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
	private ResolutionMapper resolutionMapper = new DefaultResolutionMapper();

	@InjectMocks
	@Spy
	private VersionMapper versionMapper = new DefaultVersionMapper();

	@Test
	public void testSync_NoChanges() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_IN_PROGRESS);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_1));
		targetIssue.getFields().setVersions(Collections.singleton(TARGET_VERSION_1));

		sourceIssue.getFields().setDescription("some description");
		targetIssue.getFields().setDescription(descriptionMapper.mapSourceDescription("some description"));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.UNCHANGED));

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget, jiraIssueLinker);
	}

	@Test
	public void testSync_StatusTransition() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_CLOSED);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("some description");
		targetIssue.getFields().setDescription(descriptionMapper.mapSourceDescription("some description"));

		when(jiraSource.getTransitions(sourceIssue)).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED_TRANSITION));

		JiraIssueUpdate update = expectTransitionInSource(sourceIssue);
		assertNull(update.getFields());
		assertThat(update.getTransition(), is(SOURCE_TRANSITION_RESOLVE));

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_CLOSED);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("some description");

		when(jiraSource.getTransitions(sourceIssue)).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED_TRANSITION));

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertNull(sourceIssueUpdate.getFields());
		assertThat(sourceIssueUpdate.getTransition(), is(SOURCE_TRANSITION_RESOLVE));

		JiraIssueUpdate targetIssueUpdate = expectUpdateInTarget(targetIssue);

		assertThat(targetIssueUpdate.getFields().getDescription(), is(descriptionMapper.mapTargetDescription("some description", null)));

		assertNull(targetIssueUpdate.getTransition());

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange_DoNotCopyFixedVersions() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_CLOSED);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraSource.getTransitions(sourceIssue)).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED_TRANSITION));

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertNull(sourceIssueUpdate.getFields());
		assertThat(sourceIssueUpdate.getTransition(), is(SOURCE_TRANSITION_RESOLVE));

		JiraIssueUpdate targetIssueUpdate = expectUpdateInTarget(targetIssue);

		assertThat(targetIssueUpdate.getFields().getFixVersions(), empty());
		assertNull(targetIssueUpdate.getTransition());

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange_DoCopyFixedVersions() throws Exception {
		// given
		assertThat(projectSync.getTransitions(), hasSize(1));
		projectSync.getTransitions().get(0).setCopyFixVersionsToSource(true);

		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_CLOSED);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraSource.getTransitions(sourceIssue)).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED_TRANSITION));

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueUpdate.getFields().getFixVersions(), contains(SOURCE_VERSION_2));
		assertThat(sourceIssueUpdate.getTransition(), is(SOURCE_TRANSITION_RESOLVE));

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getVersions(SOURCE_PROJECT_KEY);
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verify(jiraSource).getTransitions(sourceIssue);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_ChangedDescription() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("updated description");
		targetIssue.getFields().setDescription("some description");

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getDescription(), is(descriptionMapper.mapTargetDescription("updated description", "some description")));

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_SummaryChanged() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Other Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.UNCHANGED));

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_VersionsChanged() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_1));
		targetIssue.getFields().setVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getVersions(), is(Collections.singleton(TARGET_VERSION_1)));

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_FixVersionsChanged() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setFixVersions(new LinkedHashSet<>(Arrays.asList(SOURCE_VERSION_1, SOURCE_VERSION_2)));
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getFixVersions(), is(new LinkedHashSet<>(Arrays.asList(TARGET_VERSION_1, TARGET_VERSION_2))));

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_LabelsAndPriorityChanged() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_LOW);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setLabels(Collections.singleton("some-label"));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getLabels(), is(Collections.singleton("some-label")));
		assertThat(update.getFields().getPriority(), is(TARGET_PRIORITY_MINOR));

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_LabelsChanged_KeepCertainLabelsInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_OPEN);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setLabels(Collections.singleton("some-label"));
		targetIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("internal-label", "other-label")));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.setLabelsToKeepInTarget(Collections.singleton("internal-label"));

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getLabels(), containsInAnyOrder("some-label", "internal-label"));

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	private JiraIssueUpdate expectUpdateInTarget(JiraIssue targetIssue) {
		ArgumentCaptor<JiraIssueUpdate> jiraIssueUpdateCaptor = ArgumentCaptor.forClass(JiraIssueUpdate.class);
		verify(jiraTarget).updateIssue(eq(targetIssue), jiraIssueUpdateCaptor.capture());
		return jiraIssueUpdateCaptor.getValue();
	}

	private JiraIssueUpdate expectTransitionInSource(JiraIssue sourceIssue) {
		ArgumentCaptor<JiraIssueUpdate> jiraIssueUpdateCaptor = ArgumentCaptor.forClass(JiraIssueUpdate.class);
		verify(jiraSource).transitionIssue(eq(sourceIssue), jiraIssueUpdateCaptor.capture());
		return jiraIssueUpdateCaptor.getValue();
	}

}