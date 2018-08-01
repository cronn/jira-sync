package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class JiraIssueHistoryEntryTest {

	@Test
	public void testHasItemWithField_emptyItemList() throws Exception {
		JiraIssueHistoryEntry statusTransition = new JiraIssueHistoryEntry();
		assertThat(statusTransition.hasItemWithField("not present")).isFalse();
	}

	@Test
	public void testHasItemWithField() throws Exception {
		JiraIssueHistoryEntry statusTransition = new JiraIssueHistoryEntry()
			.addItem(JiraIssueHistoryItem.createStatusTransition("Open", "Closed"))
			.addItem(new JiraIssueHistoryItem("custom"));

		assertThat(statusTransition.hasItemWithField(WellKnownJiraField.STATUS)).isTrue();
		assertThat(statusTransition.hasItemWithField("custom")).isTrue();
		assertThat(statusTransition.hasItemWithField("not present")).isFalse();
	}

}
