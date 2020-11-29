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
public class JiraCommentTest {

	@Autowired
	private JacksonTester<JiraComment> json;

	@Test
	public void testSerialize() throws Exception {
		JiraComment comment = new JiraComment();
		comment.setCreated(ZonedDateTime.parse("2016-10-13T02:00:00+02:00"));
		comment.setUpdated(ZonedDateTime.parse("2016-10-13T07:21:13+02:00"));

		String expectedJson = "{ " +
							  "\"created\" : \"2016-10-13T02:00:00.000+0200\", " +
							  "\"updated\" : \"2016-10-13T07:21:13.000+0200\"" +
							  " }";
		assertThat(json.write(comment)).isStrictlyEqualToJson(expectedJson);
	}

	@Test
	public void testDeserialize() throws Exception {
		String json = "{ " +
					  "\"created\" : \"2016-10-13T02:00:00.000+0200\", " +
					  "\"updated\" : \"2016-10-13T07:21:13.000+0200\"" +
					  " }";
		JiraComment jiraIssue = this.json.parseObject(json);
		assertThat(jiraIssue.getCreated().toInstant()).isEqualTo(Instant.parse("2016-10-13T00:00:00Z"));
		assertThat(jiraIssue.getUpdated().toInstant()).isEqualTo(Instant.parse("2016-10-13T05:21:13Z"));
	}

}
