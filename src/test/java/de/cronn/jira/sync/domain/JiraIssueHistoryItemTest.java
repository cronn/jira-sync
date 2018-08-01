package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

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
		assertThat(statusTransition).hasToString("JiraIssueHistoryItem[field=status,fromString=from,toString=to]");
	}

	@Test
	public void testEqualsAndHashCodeContract() throws Exception {
		EqualsVerifier.forClass(JiraIssueHistoryItem.class)
			.suppress(Warning.NONFINAL_FIELDS)
			.usingGetClass()
			.verify();
	}

}
