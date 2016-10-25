package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.TestClock;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraServerInfo;
import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCommentMapperTest {

	@Mock
	private JiraService jiraSource;

	@InjectMocks
	@Spy
	private UsernameReplacer usernameReplacer = new DefaultUsernameReplacer();

	@InjectMocks
	@Spy
	private TicketReferenceReplacer ticketReferenceReplacer = new DefaultTicketReferenceReplacer();

	@InjectMocks
	private DefaultCommentMapper commentMapper;

	@Spy
	private Clock clock = new TestClock();

	@Before
	public void setUp() {
		when(jiraSource.getServerInfo()).thenReturn(new JiraServerInfo("https://jira.source"));
	}

	private JiraComment createSomeComment(String commentId) {
		JiraComment comment = new JiraComment("some text");
		comment.setId(commentId);
		comment.setAuthor(new JiraUser("user", "user", "Some User"));
		comment.setCreated(ZonedDateTime.parse("2014-05-09T13:48:52.120Z"));
		comment.setUpdated(comment.getCreated());
		return comment;
	}

	@Test
	public void testMap_HappyCase() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("1", "KEY-1");
		JiraComment comment = createSomeComment("12345");

		// when
		String body = commentMapper.map(sourceIssue, comment, jiraSource, false);

		// then
		assertThat(body).isEqualTo("{panel:title=Some User - 2014-05-09 15:48:52 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some text\n" +
			"~??[comment 12345|https://jira.source/browse/KEY-1?focusedCommentId=12345&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-12345]??~\n" +
			"{panel}");
	}

	@Test
	public void testMap_Updated() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("1", "KEY-1");
		JiraComment comment = createSomeComment("12345");
		comment.setUpdated(ZonedDateTime.parse("2014-05-10T13:41:32.088Z"));

		// when
		String body = commentMapper.map(sourceIssue, comment, jiraSource, false);

		// then
		assertThat(body).isEqualTo("{panel:title=Some User - 2014-05-09 15:48:52 CEST (Updated: 2014-05-10 15:41:32 CEST)|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some text\n" +
			"~??[comment 12345|https://jira.source/browse/KEY-1?focusedCommentId=12345&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-12345]??~\n" +
			"{panel}");
	}

	@Test
	public void testMap_outOfOrder() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("1", "KEY-1");
		JiraComment comment = createSomeComment("12345");

		// when
		String body = commentMapper.map(sourceIssue, comment, jiraSource, true);

		// then
		assertThat(body).isEqualTo("{panel:title=Some User - 2014-05-09 15:48:52 CEST|titleBGColor=#CCC|bgColor=#DDD}\n" +
			"some text\n" +
			"~??[comment 12345|https://jira.source/browse/KEY-1?focusedCommentId=12345&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-12345]??~\n" +
			"~(!) This comment was added behind time. The order of comments might not represent the real order.~\n" +
			"{panel}");
	}

	@Test
	public void testMap_baseUrlWithTrailingSlash() throws Exception {
		// given
		JiraIssue sourceIssue = new JiraIssue("1", "KEY-1");
		JiraComment comment = createSomeComment("12345");

		reset(jiraSource);
		when(jiraSource.getServerInfo()).thenReturn(new JiraServerInfo("https://jira.source/foo/"));

		// when
		String body = commentMapper.map(sourceIssue, comment, jiraSource, false);

		// then
		assertThat(body).isEqualTo("{panel:title=Some User - 2014-05-09 15:48:52 CEST|titleBGColor=#DDD|bgColor=#EEE}\n" +
			"some text\n" +
			"~??[comment 12345|https://jira.source/foo/browse/KEY-1?focusedCommentId=12345&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-12345]??~\n" +
			"{panel}");
	}

	@Test
	public void testIsMapped() throws Exception {
		JiraIssue sourceIssue = new JiraIssue("1", "KEY-1");
		JiraComment comment1 = createSomeComment("100");
		JiraComment comment2 = createSomeComment("1001");

		String mappedComment1 = commentMapper.map(sourceIssue, comment1, jiraSource, false);
		String mappedComment2 = commentMapper.map(sourceIssue, comment2, jiraSource, false);

		assertThat(commentMapper.isMapped(comment1, mappedComment1)).isTrue();
		assertThat(commentMapper.isMapped(comment2, mappedComment2)).isTrue();

		assertThat(commentMapper.isMapped(comment1, mappedComment2)).isFalse();
		assertThat(commentMapper.isMapped(comment2, mappedComment1)).isFalse();

		assertThat(commentMapper.isMapped(comment1, "")).isFalse();
		assertThat(commentMapper.isMapped(comment1, "foobar")).isFalse();
	}

	@Test
	public void testWasAddedBehindTime() throws Exception {
		assertThat(commentMapper.wasAddedBehindTime(new JiraComment(""))).isFalse();
		assertThat(commentMapper.wasAddedBehindTime(new JiraComment("some text behind time"))).isFalse();
		assertThat(commentMapper.wasAddedBehindTime(new JiraComment("This comment was added behind time"))).isTrue();
	}

}