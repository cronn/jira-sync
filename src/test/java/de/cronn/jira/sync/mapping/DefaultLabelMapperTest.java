package de.cronn.jira.sync.mapping;

import static de.cronn.jira.sync.SetUtils.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;

public class DefaultLabelMapperTest {

	private LabelMapper labelMapper;

	@Before
	public void setUp() {
		labelMapper = new DefaultLabelMapper();
	}

	@Test
	public void testMapLabels_HappyPath() throws Exception {
		// given
		JiraIssue jiraIssue = new JiraIssue();
		jiraIssue.setFields(new JiraIssueFields());
		jiraIssue.getFields().setLabels(newLinkedHashSet("label1", "label2", "label3"));

		// when
		Set<String> labels = labelMapper.mapLabels(jiraIssue);

		// then
		assertThat(jiraIssue.getFields().getLabels()).isNotSameAs(labels);
		assertThat(labels).containsExactly("label1", "label2", "label3");
	}

	@Test
	public void testMapLabels_Null() throws Exception {
		// given
		JiraIssue jiraIssue = new JiraIssue();
		jiraIssue.setFields(new JiraIssueFields());

		// when
		Set<String> labels = labelMapper.mapLabels(jiraIssue);

		// then
		assertThat(labels).isEmpty();
	}

}