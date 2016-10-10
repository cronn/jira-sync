package de.cronn.jira.sync.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueStatus;

public class SummaryMapperTest {

	private static final JiraIssueStatus OPEN = new JiraIssueStatus("1", "Open");

	@Test
	public void testMapSummary_HappyCase() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("0", "TEST-123", "Some Summary", OPEN);

		// when
		String summary = SummaryMapper.mapSummary(issue);

		// then
		assertThat(summary, is("TEST-123: Some Summary"));
	}

	@Test
	public void testMapSummary_EmptySummary() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("0", "TEST-123", "", OPEN);

		// when
		String summary = SummaryMapper.mapSummary(issue);

		// then
		assertThat(summary, is("TEST-123: "));
	}

	@Test
	public void testMapSummary_NullKey() throws Exception {
		JiraIssue issue = new JiraIssue("0", null, "Some Summary", OPEN);

		try {
			SummaryMapper.mapSummary(issue);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), is("key must not be null"));
		}
	}

	@Test
	public void testMapSummary_NullSummary() throws Exception {
		JiraIssue issue = new JiraIssue("0", "TEST-123", null, OPEN);

		try {
			SummaryMapper.mapSummary(issue);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), is("summary must not be null"));
		}
	}

}