package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JsonTest
public class JiraIssueTest {

	@Autowired
	private JacksonTester<JiraIssue> json;

	@Test
	public void testSerialize() throws Exception {
		JiraIssue issue = new JiraIssue("1", "ISSUE-1");
		issue.getOrCreateFields().setUpdated(ZonedDateTime.parse("2016-10-13T07:21:13+02:00"));

		String expectedJson = "{ \"id\" : \"1\", \"key\" : \"ISSUE-1\", " +
			"\"fields\" : {" +
				"\"updated\" : \"2016-10-13T07:21:13.000+0200\"" +
			"} }";
		assertThat(json.write(issue)).isStrictlyEqualToJson(expectedJson);
	}

	@Test
	public void testDeserialize() throws Exception {
		String json = "{ \"id\" : \"1\"," +
			"\"key\" : \"ISSUE-1\"," +
			"\"fields\" : {" +
			" \"updated\" : \"2016-10-13T09:21:13.000+0200\"" +
			"}," +
			"\"other\" : \"is ignored\" }";
		JiraIssue jiraIssue = this.json.parseObject(json);
		assertThat(jiraIssue.getId()).isEqualTo("1");
		assertThat(jiraIssue.getKey()).isEqualTo("ISSUE-1");
		assertThat(jiraIssue.getFields().getUpdated().toInstant()).isEqualTo(Instant.parse("2016-10-13T07:21:13Z"));
	}

	@Test
	public void testToString() {
		JiraIssue issue = new JiraIssue("1", "KEY-123");
		assertThat(issue).hasToString("JiraIssue[id=1,key=KEY-123]");
	}

}