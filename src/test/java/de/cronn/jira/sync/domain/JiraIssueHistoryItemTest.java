package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class JiraIssueHistoryItemTest {

	@Test
	public void testCreateStatusTransition() throws Exception {
		JiraIssueHistoryItem statusTransition = JiraIssueHistoryItem.createStatusTransition("from", "to");

		assertThat(statusTransition.getField()).isEqualTo(WellKnownJiraField.STATUS.getFieldName());
		assertThat(statusTransition.getFromString()).isEqualTo("from");
		assertThat(statusTransition.getToString()).isEqualTo("to");
	}

	@Test
	public void testToString() throws Exception {
		JiraIssueHistoryItem statusTransition = JiraIssueHistoryItem.createStatusTransition("from", "to");
		assertThat(statusTransition.toString()).isEqualTo("JiraIssueHistoryItem[field=status,fromString=from,toString=to]");
	}

}
