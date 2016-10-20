package de.cronn.jira.sync.strategy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.TestClock;
import de.cronn.jira.sync.config.TransitionConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;
import de.cronn.jira.sync.domain.JiraIssueUpdate;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.mapping.CommentMapper;
import de.cronn.jira.sync.mapping.DefaultCommentMapper;
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
import de.cronn.jira.sync.service.JiraService;

public class UpdateExistingTargetJiraIssueSyncStrategyTest extends AbstractIssueSyncStrategyTest {

	@Mock
	private JiraIssueLinker jiraIssueLinker;

	@Spy
	private Clock clock = new TestClock();

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

	@InjectMocks
	@Spy
	private CommentMapper commentMapper = new DefaultCommentMapper();

	@Test
	public void testSync_NoChanges() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_IN_PROGRESS, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_1));
		targetIssue.getFields().setVersions(Collections.singleton(TARGET_VERSION_1));

		sourceIssue.getFields().setDescription("some description");
		targetIssue.getFields().setDescription(descriptionMapper.mapSourceDescription("some description"));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.UNCHANGED);

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget, jiraIssueLinker);
	}

	@Test
	public void testSync_StatusTransition() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("some description");
		targetIssue.getFields().setDescription(descriptionMapper.mapSourceDescription("some description"));

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate update = expectTransitionInSource(sourceIssue);
		assertThat(update.getFields()).isNull();
		assertThat(update.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("some description");

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueUpdate.getFields()).isNull();
		assertThat(sourceIssueUpdate.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		JiraIssueUpdate targetIssueUpdate = expectUpdateInTarget(targetIssue);

		assertThat(targetIssueUpdate.getFields().getDescription()).isEqualTo(descriptionMapper.mapTargetDescription("some description", null));

		assertThat(targetIssueUpdate.getTransition()).isNull();

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange_DoNotCopyFixedVersions() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueUpdate.getFields()).isNull();
		assertThat(sourceIssueUpdate.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		JiraIssueUpdate targetIssueUpdate = expectUpdateInTarget(targetIssue);

		assertThat(targetIssueUpdate.getFields().getFixVersions()).isEmpty();
		assertThat(targetIssueUpdate.getTransition()).isNull();

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransition_AssignToMyself() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.getTransition(TRANSITION_RESOLVE).setAssignToMyselfInSource(true);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate sourceIssueUpdate = expectUpdateInSource(sourceIssue);
		assertThat(sourceIssueUpdate.getFields().getAssignee()).isEqualTo(SOURCE_USER_MYSELF);
		assertThat(sourceIssueUpdate.getTransition()).isNull();

		JiraIssueUpdate sourceIssueTransition = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueTransition.getFields()).isNull();
		assertThat(sourceIssueTransition.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		verify(jiraSource).getMyself();
		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_StatusTransition_AssignToMyself_AlreadyAssignedToMyself() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		sourceIssue.getOrCreateFields().setAssignee(SOURCE_USER_MYSELF);

		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.getTransition(TRANSITION_RESOLVE).setAssignToMyselfInSource(true);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate sourceIssueTransition = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueTransition.getFields()).isNull();
		assertThat(sourceIssueTransition.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		verify(jiraSource).getMyself();
		verify(jiraTarget).getPriorities();
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_IllegalNumberOfTransitions() throws Exception {
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Arrays.asList(SOURCE_TRANSITION_RESOLVE, SOURCE_TRANSITION_CLOSE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.addTransition("closeTransition",
			new TransitionConfig(
				Collections.singletonList(SOURCE_STATUS_OPEN.getName()),
				Collections.singletonList(TARGET_STATUS_CLOSED.getName()),
				SOURCE_STATUS_CLOSED.getName()
			)
		);

		try {
			strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e).hasMessage("Illegal number of matching transitions: " +
				"[TransitionConfig[sourceStatusIn=[Open],targetStatusIn=[Closed],sourceStatusToSet=Resolved], " +
				"TransitionConfig[sourceStatusIn=[Open],targetStatusIn=[Closed],sourceStatusToSet=Closed]]");
		}
	}

	@Test
	public void testSync_NoTransitionToRequiredStatus() throws Exception {
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.emptyList());

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		try {
			strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e).hasMessage("Found no transition to status 'Resolved'");
		}
	}

	@Test
	public void testSync_MultipleTransitionsToRequiredStatus() throws Exception {
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_CLOSED, TARGET_PRIORITY_MAJOR);

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Arrays.asList(SOURCE_TRANSITION_RESOLVE, SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		try {
			strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e).hasMessageStartingWith("Found multiple transitions to status 'Resolved'");
		}
	}

	@Test
	public void testSync_StatusTransitionAndFieldChange_DoCopyFixedVersions() throws Exception {
		// given
		assertThat(projectSync.getTransitions().keySet()).hasSize(1);
		projectSync.getTransition(TRANSITION_RESOLVE).setCopyFixVersionsToSource(true);

		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", SOURCE_STATUS_OPEN);
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", TARGET_STATUS_CLOSED);

		sourceIssue.getFields().setPriority(SOURCE_PRIORITY_HIGH);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraSource.getTransitions(sourceIssue.getKey())).thenReturn(Collections.singletonList(SOURCE_TRANSITION_RESOLVE));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED_TRANSITION);

		JiraIssueUpdate sourceIssueUpdate = expectTransitionInSource(sourceIssue);
		assertThat(sourceIssueUpdate.getFields().getFixVersions()).containsExactly(SOURCE_VERSION_2);
		assertThat(sourceIssueUpdate.getTransition()).isEqualTo(SOURCE_TRANSITION_RESOLVE);

		verify(jiraTarget).getPriorities();
		verify(jiraSource).getVersions(SOURCE_PROJECT_KEY);
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verify(jiraSource).getTransitions(sourceIssue.getKey());
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_ChangedDescription() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_OPEN, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setDescription("updated description");
		targetIssue.getFields().setDescription("some description");

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED);

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getDescription()).isEqualTo(descriptionMapper.mapTargetDescription("updated description", "some description"));

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_SummaryChanged() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);

		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Other Summary", TARGET_STATUS_OPEN);
		targetIssue.getFields().setPriority(TARGET_PRIORITY_MAJOR);

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.UNCHANGED);

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_VersionsChanged() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_OPEN, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setVersions(Collections.singleton(SOURCE_VERSION_1));
		targetIssue.getFields().setVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED);

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getVersions()).containsExactly(TARGET_VERSION_1);

		verify(jiraTarget).getPriorities();
		verify(jiraTarget).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_FixVersionsChanged() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_OPEN, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setFixVersions(new LinkedHashSet<>(Arrays.asList(SOURCE_VERSION_1, SOURCE_VERSION_2)));
		targetIssue.getFields().setFixVersions(Collections.singleton(TARGET_VERSION_2));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED);

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getFixVersions()).containsExactly(TARGET_VERSION_1, TARGET_VERSION_2);

		verify(jiraTarget).getPriorities();
		verify(jiraTarget, atLeast(1)).getVersions(TARGET_PROJECT_KEY);
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_LabelsAndPriorityChanged() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_LOW);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_OPEN, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setLabels(Collections.singleton("some-label"));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED);

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getLabels()).containsExactly("some-label");
		assertThat(update.getFields().getPriority()).isEqualTo(TARGET_PRIORITY_MINOR);

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	@Test
	public void testSync_LabelsChanged_KeepCertainLabelsInTarget() throws Exception {
		// given
		JiraIssue sourceIssue = newSourceIssue(SOURCE_STATUS_OPEN, SOURCE_PRIORITY_HIGH);
		JiraIssue targetIssue = newTargetIssue(TARGET_STATUS_OPEN, TARGET_PRIORITY_MAJOR);

		sourceIssue.getFields().setLabels(Collections.singleton("some-label"));
		targetIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("internal-label", "other-label")));

		when(jiraIssueLinker.resolve(targetIssue, jiraTarget, jiraSource)).thenReturn(sourceIssue);

		projectSync.setLabelsToKeepInTarget(Collections.singleton("internal-label"));

		// when
		SyncResult result = strategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);

		// then
		assertThat(result).isEqualTo(SyncResult.CHANGED);

		JiraIssueUpdate update = expectUpdateInTarget(targetIssue);
		assertThat(update.getFields().getLabels()).containsExactlyInAnyOrder("some-label", "internal-label");

		verify(jiraTarget).getPriorities();
		verifyNoMoreInteractions(jiraSource, jiraTarget);
	}

	private JiraIssue newTargetIssue(JiraIssueStatus status, JiraPriority priority) {
		JiraIssue targetIssue = new JiraIssue("400", "TARGET-123", "Some Summary", status);
		targetIssue.getFields().setPriority(priority);
		return targetIssue;
	}

	private JiraIssue newSourceIssue(JiraIssueStatus status, JiraPriority priority) {
		JiraIssue sourceIssue = new JiraIssue("100", "SOURCE-123", "Some Summary", status);
		sourceIssue.getFields().setPriority(priority);
		return sourceIssue;
	}

	private JiraIssueUpdate expectUpdateInTarget(JiraIssue targetIssue) {
		return expectUpdate(jiraTarget, targetIssue);
	}

	private JiraIssueUpdate expectUpdateInSource(JiraIssue sourceIssue) {
		return expectUpdate(jiraSource, sourceIssue);
	}

	private JiraIssueUpdate expectUpdate(JiraService jiraService, JiraIssue issue) {
		ArgumentCaptor<JiraIssueUpdate> jiraIssueUpdateCaptor = ArgumentCaptor.forClass(JiraIssueUpdate.class);
		verify(jiraService).updateIssue(eq(issue.getKey()), jiraIssueUpdateCaptor.capture());
		return jiraIssueUpdateCaptor.getValue();
	}

	private JiraIssueUpdate expectTransitionInSource(JiraIssue sourceIssue) {
		ArgumentCaptor<JiraIssueUpdate> jiraIssueUpdateCaptor = ArgumentCaptor.forClass(JiraIssueUpdate.class);
		verify(jiraSource).transitionIssue(eq(sourceIssue.getKey()), jiraIssueUpdateCaptor.capture());
		return jiraIssueUpdateCaptor.getValue();
	}

}