package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;

public class DefaultSummaryMapperTest {

	private static final JiraIssueStatus OPEN = new JiraIssueStatus("1", "Open");

	private SummaryMapper summaryMapper;

	@Before
	public void setUp() {
		summaryMapper = new DefaultSummaryMapper();
	}

	@Test
	public void testMapSummary_HappyCase() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("0", "TEST-123", "Some Summary", OPEN);

		// when
		String summary = summaryMapper.mapSummary(issue);

		// then
		assertThat(summary).isEqualTo("TEST-123: Some Summary");
	}

	@Test
	public void testMapSummary_EmptySummary() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("0", "TEST-123", "", OPEN);

		// when
		String summary = summaryMapper.mapSummary(issue);

		// then
		assertThat(summary).isEqualTo("TEST-123: ");
	}

	@Test
	public void testMapSummary_NullKey() throws Exception {
		JiraIssue issue = new JiraIssue("0", null, "Some Summary", OPEN);

		try {
			summaryMapper.mapSummary(issue);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("key must not be null");
		}
	}

	@Test
	public void testMapSummary_NullSummary() throws Exception {
		JiraIssue issue = new JiraIssue("0", "TEST-123", null, OPEN);

		try {
			summaryMapper.mapSummary(issue);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("summary must not be null");
		}
	}

}