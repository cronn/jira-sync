package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JiraIssueTest {

	@Test
	public void testToString() {
		JiraIssue issue = new JiraIssue("1", "KEY-123");
		assertThat(issue.toString(), is("JiraIssue[id=1,key=KEY-123]"));
	}

}