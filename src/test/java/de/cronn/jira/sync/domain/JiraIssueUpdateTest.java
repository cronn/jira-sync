package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JsonTest
public class JiraIssueUpdateTest {

	@Autowired
	private JacksonTester<JiraIssueUpdate> json;

	@Test
	public void testSerialize_Empty() throws Exception {
		JiraIssueUpdate issue = new JiraIssueUpdate();

		String expectedJson = "{}";
		assertThat(json.write(issue)).isStrictlyEqualToJson(expectedJson);
	}

	@Test
	public void testSerialize_ChangedDescription() throws Exception {
		JiraIssueUpdate issue = new JiraIssueUpdate();
		issue.getOrCreateFields().setDescription("new description");

		String expectedJson = "{ \"fields\" : { \"description\" : \"new description\" } }";
		assertThat(json.write(issue)).isStrictlyEqualToJson(expectedJson);
	}

}