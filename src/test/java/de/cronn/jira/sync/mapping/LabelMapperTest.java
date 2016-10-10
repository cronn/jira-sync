package de.cronn.jira.sync.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;

public class LabelMapperTest {

	@Test
	public void testMapLabels_HappyPath() throws Exception {
		// given
		JiraIssue jiraIssue = new JiraIssue();
		jiraIssue.setFields(new JiraIssueFields());
		jiraIssue.getFields().setLabels(new LinkedHashSet<>(Arrays.asList("label1", "label2", "label3")));

		// when
		Set<String> labels = LabelMapper.mapLabels(jiraIssue);

		// then
		assertNotSame(jiraIssue.getFields().getLabels(), labels);
		assertThat(labels, contains("label1", "label2", "label3"));
	}

	@Test
	public void testMapLabels_Null() throws Exception {
		// given
		JiraIssue jiraIssue = new JiraIssue();
		jiraIssue.setFields(new JiraIssueFields());

		// when
		Set<String> labels = LabelMapper.mapLabels(jiraIssue);

		// then
		assertThat(labels, empty());
	}

}