package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTicketReferenceReplacerTest {

	@Mock
	private JiraService jiraService;

	private DefaultTicketReferenceReplacer ticketReferenceReplacer;

	@Before
	public void setUp() {
		ticketReferenceReplacer = new DefaultTicketReferenceReplacer();

		when(jiraService.getServerInfo()).thenReturn(new JiraServerInfo("https://jira"));
		when(jiraService.getProjects()).thenReturn(Arrays.asList(
			new JiraProject("1", "PROJECT"),
			new JiraProject("2", "OTHER")
		));
	}

	@Test
	public void testEmptyInput() throws Exception {
		assertThat(replace(null)).isNull();
		assertThat(replace("")).isEqualTo("");

		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithoutTicketReference() throws Exception {
		// given
		String inputText = "some text with out ticket FOO-123 reference";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with out ticket FOO-123 reference");

		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithTicketReference() throws Exception {
		// given
		String inputText = "some text with PROJECT-123 reference";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with [PROJECT-123|https://jira/browse/PROJECT-123] reference");

		verify(jiraService).getServerInfo();
		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithTicketReferenceInSentence() throws Exception {
		assertThat(replace("Isn’t it a duplicate of PROJECT-123?"))
			.isEqualTo("Isn’t it a duplicate of [PROJECT-123|https://jira/browse/PROJECT-123]?");

		assertThat(replace("This is a duplicate of PROJECT-123!"))
			.isEqualTo("This is a duplicate of [PROJECT-123|https://jira/browse/PROJECT-123]!");

		assertThat(replace("This is a duplicate of PROJECT-123;PROJECT-456;PROJECT-768"))
			.isEqualTo("This is a duplicate of [PROJECT-123|https://jira/browse/PROJECT-123];" +
				"[PROJECT-456|https://jira/browse/PROJECT-456];" +
				"[PROJECT-768|https://jira/browse/PROJECT-768]");

		assertThat(replace("This could be a duplicate of PROJECT-123…"))
			.isEqualTo("This could be a duplicate of [PROJECT-123|https://jira/browse/PROJECT-123]…");

		assertThat(replace("I think this is a duplicate of PROJECT-123."))
			.isEqualTo("I think this is a duplicate of [PROJECT-123|https://jira/browse/PROJECT-123].");

		verify(jiraService, atLeast(1)).getServerInfo();
		verify(jiraService, atLeast(1)).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithTicketReferenceAtBeginning() throws Exception {
		// given
		String inputText = "PROJECT-123: some text with reference";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("[PROJECT-123|https://jira/browse/PROJECT-123]: some text with reference");

		verify(jiraService).getServerInfo();
		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithTicketReferenceAtBeginningAndEnd() throws Exception {
		// given
		String inputText = "PROJECT-123";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("[PROJECT-123|https://jira/browse/PROJECT-123]");

		verify(jiraService).getServerInfo();
		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithTicketLink() throws Exception {
		// given
		String inputText = "some text with https://jira/browse/PROJECT-123 reference";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with https://jira/browse/PROJECT-123 reference");

		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testTextWithMultipleTicketReferences() throws Exception {
		// given
		String inputText = "some text with PROJECT-123 reference and another reference: OTHER-456";

		// when
		String output = replace(inputText);

		// then
		assertThat(output).isEqualTo("some text with [PROJECT-123|https://jira/browse/PROJECT-123] reference and another reference: [OTHER-456|https://jira/browse/OTHER-456]");

		verify(jiraService, atLeast(1)).getServerInfo();
		verify(jiraService).getProjects();
		verifyNoMoreInteractions(jiraService);
	}

	private String replace(String inputText) {
		return ticketReferenceReplacer.replaceTicketReferences(inputText, jiraService);
	}


}