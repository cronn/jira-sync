package de.cronn.jira.sync.resolve;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class JiraIssueByExternalLinkResolverTest {

	private static final String JIRA_SOURCE_URL = "https://jira.source";
	private static final String JIRA_TARGET_URL = "https://jira.target";

	@Mock
	private JiraService jiraSource;

	@Mock
	private JiraService jiraTarget;

	@Before
	public void setUp() throws Exception {
		when(jiraSource.getUrl()).thenReturn(new URL(JIRA_SOURCE_URL));
		when(jiraTarget.getUrl()).thenReturn(new URL(JIRA_TARGET_URL));

		when(jiraSource.getServerInfo()).thenReturn(new JiraServerInfo(JIRA_SOURCE_URL));
		when(jiraTarget.getServerInfo()).thenReturn(new JiraServerInfo(JIRA_TARGET_URL));
	}

	@Test
	public void testResolve_NoRemoteLinks() throws Exception {

		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue jiraIssue = new JiraIssue();

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		when(jiraSource.getRemoteLinks(jiraIssue)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolve(jiraIssue, jiraSource, jiraTarget);
		assertNull(resolvedIssue);
	}

	@Test
	public void testResolve_OtherRemoteLinks() throws Exception {
		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue jiraIssue = new JiraIssue();

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink("http://some.thing"));
		remoteLinks.add(new JiraRemoteLink("http://other.thing"));
		when(jiraSource.getRemoteLinks(jiraIssue)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolve(jiraIssue, jiraSource, jiraTarget);
		assertNull(resolvedIssue);
	}

	@Test
	public void testResolve_HappyPath() throws Exception {
		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue sourceIssue = new JiraIssue("1", "SOURCE-12");
		JiraIssue targetIssue = new JiraIssue("1", "TARGET-123");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(targetIssue);

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink("http://some.thing"));
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolve(sourceIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue, sameInstance(targetIssue));

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_ExtraSlash() throws Exception {
		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue sourceIssue = new JiraIssue("1", "SOURCE-12");
		JiraIssue targetIssue = new JiraIssue("1", "TARGET-123");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(targetIssue);

		List<JiraRemoteLink> remoteLinks = Collections.singletonList(new JiraRemoteLink("https://jira.target//browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue)).thenReturn(remoteLinks);

		JiraIssue resolvedIssue = resolver.resolve(sourceIssue, jiraSource, jiraTarget);
		assertThat(resolvedIssue, sameInstance(targetIssue));

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_CannotResolveFromTarget() throws Exception {
		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue sourceIssue = new JiraIssue("1", "SOURCE-123");
		JiraIssue targetIssue = new JiraIssue("1", "TARGET-123");

		when(jiraTarget.getIssueByKey(targetIssue.getKey())).thenReturn(null);

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue)).thenReturn(remoteLinks);

		try {
			resolver.resolve(sourceIssue, jiraSource, jiraTarget);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e.getMessage(), is("Failed to resolve TARGET-123 in target"));
		}

		verify(jiraTarget).getIssueByKey(targetIssue.getKey());
	}

	@Test
	public void testResolve_MultipleRemoteIssues() throws Exception {
		JiraIssueResolver resolver = new JiraIssueByExternalLinkResolver();

		JiraIssue sourceIssue = new JiraIssue("1", "SOURCE-123");
		JiraIssue targetIssue1 = new JiraIssue("1", "TARGET-123");
		JiraIssue targetIssue2 = new JiraIssue("2", "TARGET-456");

		when(jiraTarget.getIssueByKey(targetIssue1.getKey())).thenReturn(targetIssue1);
		when(jiraTarget.getIssueByKey(targetIssue2.getKey())).thenReturn(targetIssue2);

		List<JiraRemoteLink> remoteLinks = new ArrayList<>();
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue1.getKey()));
		remoteLinks.add(new JiraRemoteLink(JIRA_TARGET_URL + "/browse/" + targetIssue2.getKey()));
		when(jiraSource.getRemoteLinks(sourceIssue)).thenReturn(remoteLinks);

		try {
			resolver.resolve(sourceIssue, jiraSource, jiraTarget);
			fail("JiraSyncException expected");
		} catch (JiraSyncException e) {
			assertThat(e.getMessage(), is("Illegal number of linked jira issues for JiraIssue[id=1,key=SOURCE-123]: [JiraIssue[id=1,key=TARGET-123], JiraIssue[id=2,key=TARGET-456]]"));
		}
	}

}