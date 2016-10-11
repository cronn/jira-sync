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

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.mapping.DefaultDescriptionMapper;
import de.cronn.jira.sync.mapping.DefaultIssueTypeMapper;
import de.cronn.jira.sync.mapping.DefaultLabelMapper;
import de.cronn.jira.sync.mapping.DefaultPriorityMapper;
import de.cronn.jira.sync.mapping.DefaultResolutionMapper;
import de.cronn.jira.sync.mapping.IssueTypeMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.ResolutionMapper;
import de.cronn.jira.sync.resolve.JiraIssueResolver;

public class UpdateExistingTargetJiraIssueSyncStrategyTest extends AbstractIssueSyncStrategyTest {

	@Mock
	private JiraIssueResolver jiraIssueResolver;

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.UNCHANGED));

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verify(jiraIssueResolver).resolve(targetIssue, jiraTarget, jiraSource);
		verifyNoMoreInteractions(jiraSource, jiraTarget, jiraIssueResolver);
	}

	@Test
	public void testSync_MissingBacklink() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_IN_PROGRESS);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.UNCHANGED));

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).addRemoteLink(targetIssue, sourceIssue, jiraSource, projectSync.getRemoteLinkIconInTarget());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_IllegalBacklink() throws Exception {
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue otherSourceIssue = new JiraIssue("200", "SOURCE-500", "Other", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_IN_PROGRESS);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(otherSourceIssue);

		try {
			strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e.getMessage(), is("Backlink of JiraIssue[id=400,key=TARGET-123] points to JiraIssue[id=200,key=SOURCE-500]; expected: JiraIssue[id=100,key=SOURCE-123]"));
		}
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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED_TRANSITION));

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertNull(sourceIssueUpdate.getFields());
		assertThat(sourceIssueUpdate.getTransition(), is(SOURCE_TRANSITION_RESOLVE));

		JiraIssueUpdate targetIssueUpdate = expectUpdateInTarget(targetIssue);

		assertThat(targetIssueUpdate.getFields().keySet(), containsInAnyOrder("description"));
		assertThat(targetIssueUpdate.getFields().values(), containsInAnyOrder(descriptionMapper.mapTargetDescription("some description", null)));

		assertNull(targetIssueUpdate.getTransition());

		verify(jiraTarget).getPriorities();
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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().keySet(), containsInAnyOrder("description"));
		assertThat(update.getFields().values(), containsInAnyOrder(descriptionMapper.mapTargetDescription("updated description", "some description")));

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().keySet(), contains("versions"));
		assertThat(update.getFields().values(), contains(Collections.singleton(TARGET_VERSION_1)));

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().keySet(), contains("fixVersions"));
		assertThat(update.getFields().values(), contains(new LinkedHashSet<>(Arrays.asList(TARGET_VERSION_1, TARGET_VERSION_2))));

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().keySet(), contains("labels", "priority"));
		assertThat(update.getFields().values(), contains(
			Collections.singleton("some-label"),
			TARGET_PRIORITY_MINOR
		));

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

		when(jiraIssueResolver.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.setLabelsToKeepInTarget(Collections.singleton("internal-label"));

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result, is(SyncResult.CHANGED));

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().keySet(), contains("labels"));
		assertThat(update.getFields().values(), contains(
			new LinkedHashSet<>(Arrays.asList("internal-label", "some-label"))
		));

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