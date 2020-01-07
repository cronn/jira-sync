package de.cronn.jira.sync.link;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueWebLinkerTest {

	private static final String JIRA_TARGET_URL = "https://jira.target";

	private static final Instant UPDATED = Instant.parse("2016-01-01T00:00:00.000Z");

	@Mock
	private JiraService jiraSource;

	@Mock
	private JiraService jiraTarget;

	@Before
	public void setUp() throws Exception {
		when(jiraTarget.getServerInfo()).thenReturn(new JiraServerInfo(JIRA_TARGET_URL));
	}

	@Test
	public void testResolve_NoRemoteLinks() throws Exception {

		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue jiraIssue = createJiraIssue("1", "KEY-1");

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		when(jiraSource.getRemoteLinks(jiraIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolveIssue(jiraIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue).isNull();
	}

	private JiraIssue createJiraIssue(String key, String id) {
		JiraIssue issue = new JiraIssue(id, key);
		issue.setFields(new JiraIssueFields());
		issue.getFields().setUpdated(ZonedDateTime.ofInstant(UPDATED, ZoneOffset.UTC));
		return issue;
	}

	@Test
	public void testResolve_OtherRemoteLinks() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue jiraIssue = createJiraIssue("1", "KEY-1");

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink("http://some.thing"));
		remoteLinks.add(new JiraRemoteLink("http://other.thing"));
		when(jiraSource.getRemoteLinks(jiraIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolveIssue(jiraIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue).isNull();
	}

	@Test
	public void testResolve_HappyPath() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue sourceIssue = createJiraIssue("SOURCE-12", "1");
		JiraIssue targetIssue = createJiraIssue("TARGET-123", "1");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(targetIssue);

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink("http://some.thing"));
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolveIssue(sourceIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue).isSameAs(targetIssue);

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_ExtraSlash() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue sourceIssue = createJiraIssue("SOURCE-12", "1");
		JiraIssue targetIssue = createJiraIssue("TARGET-123", "1");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(targetIssue);

		List<JiraRemoteLink> remoteLinks = Collections.singletonList(new JiraRemoteLink("https://jira.target//browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolveIssue(sourceIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue).isSameAs(targetIssue);

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_CannotResolveFromTarget() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue sourceIssue = createJiraIssue("SOURCE-123", "1");
		JiraIssue targetIssue = createJiraIssue("TARGET-123", "1");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(null);

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> resolver.resolveIssue(sourceIssue, jiraSource, jiraTarget))
			.withMessage("Failed to resolve 'TARGET-123' in jiraTarget");

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_MultipleRemoteIssues() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue sourceIssue = createJiraIssue("SOURCE-123", "1");
		JiraIssue targetIssue1 = createJiraIssue("TARGET-123", "1");
		JiraIssue targetIssue2 = createJiraIssue("TARGET-456", "2");

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue1.getKey()));
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue2.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue.getKey(), UPDATED)).thenReturn(remoteLinks);

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> resolver.resolveIssue(sourceIssue, jiraSource, jiraTarget))
			.withMessage("Illegal number of linked issues for JiraIssue[id=1,key=SOURCE-123]: [TARGET-123, TARGET-456]");
	}

	@Test
	public void testResolve_FailedToGetRemoteLinks() throws Exception {
		JiraIssueLinker resolver = new JiraIssueWebLinker();

		JiraIssue sourceIssue = createJiraIssue("SOURCE-123", "1");

		when(jiraSource.getRemoteLinks(sourceIssue.getKey(), UPDATED)).thenThrow(new JiraSyncException("You do not have the permission to see the specified issue."));

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> resolver.resolveIssue(sourceIssue, jiraSource, jiraTarget))
			.withMessage("Failed to resolved keys for JiraIssue[id=1,key=SOURCE-123]")
			.withStackTraceContaining("You do not have the permission to see the specified issue.");
	}

}
