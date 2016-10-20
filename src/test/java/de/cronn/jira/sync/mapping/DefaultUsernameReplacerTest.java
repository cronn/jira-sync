package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultUsernameReplacerTest {

	@Mock
	private JiraService jiraService;

	private DefaultUsernameReplacer usernameReplacer;

	@Before
	public void setUp() {
		usernameReplacer = new DefaultUsernameReplacer();

		when(jiraService.getServerInfo()).thenReturn(new JiraServerInfo("https://jira"));
	}

	@Test
	public void testEmptyInput() throws Exception {
		assertThat(replace(null)).isNull();
		assertThat(replace("")).isEqualTo("");
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithoutUsername() throws Exception {
		// given
		String inputText = "some text [ with out username ] ~bla";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text [ with out username ] ~bla");

		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithUsername() throws Exception {
		// given
		String inputText = "some text with [~some.user] reference";
		when(jiraService.getUserByName("some.user")).thenReturn(new JiraUser("some.user", "some.user", "Some User"));

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with [Some User|https://jira/secure/ViewProfile.jspa?name=some.user] reference");

		verify(jiraService).getUserByName("some.user");
		verify(jiraService).getServerInfo();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithMultipleUsernames() throws Exception {
		// given
		String inputText = "some text with [~some.user] reference and another reference: [~anotherUser1]";
		when(jiraService.getUserByName("some.user")).thenReturn(new JiraUser("some.user", "some.user", "Some User"));
		when(jiraService.getUserByName("anotherUser1")).thenReturn(new JiraUser("anotherUser1", "anotherUser1", "Another User"));

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with [Some User|https://jira/secure/ViewProfile.jspa?name=some.user] reference and another reference: [Another User|https://jira/secure/ViewProfile.jspa?name=anotherUser1]");

		verify(jiraService).getUserByName("some.user");
		verify(jiraService).getUserByName("anotherUser1");
		verify(jiraService, atLeast(1)).getServerInfo();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithUnknownUsername() throws Exception {
		// given
		String inputText = "some text with [~unknown.user] reference";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with [~unknown.user] reference");

		verify(jiraService).getUserByName("unknown.user");
		verifyNoMoreInteractions(jiraService);
	}

	private String replace(String inputText) {
		return usernameReplacer.replaceUsernames(inputText, jiraService);
	}


}