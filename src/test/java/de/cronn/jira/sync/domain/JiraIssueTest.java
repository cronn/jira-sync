package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

public class JiraIssueTest {

	@Test
	public void testToString() {
		JiraIssue issue = new JiraIssue("1", "KEY-123");
		assertThat(issue.toString(), is("JiraIssue[id=1,key=KEY-123]"));
	}

	@Test
	public void updatedTimestamp() throws Exception {
		new ISO8601DateFormat().parse("2016-10-13T09:21:13.000+0200");
	}

}