package de.cronn.jira.sync.domain;

import java.time.ZonedDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JiraChangeLogTest {

	@Test
	public void testGetLatestStatusTransition_Null() throws Exception {
		JiraChangeLog changelog = new JiraChangeLog();
		Assertions.assertThat(changelog.getLatestStatusTransition()).isNull();
	}

	@Test
	public void testGetLatestStatusTransition() throws Exception {
		JiraIssueHistoryEntry oldStatusTransition = new JiraIssueHistoryEntry("1")
			.withCreated(ZonedDateTime.parse("2016-09-13T02:00:00+02:00"))
			.addItem(JiraIssueHistoryItem.createStatusTransition("In Progress", "Resolved"));
		
		JiraIssueHistoryEntry latestStatusTransition = new JiraIssueHistoryEntry("1")
			.withCreated(ZonedDateTime.parse("2016-10-13T02:00:00+02:00"))
			.addItem(JiraIssueHistoryItem.createStatusTransition("In Progress", "Resolved"));
		
		JiraIssueHistoryEntry notAStatusTransition = new JiraIssueHistoryEntry("1")
			.withCreated(ZonedDateTime.parse("2015-10-13T02:00:00+02:00"))
			.addItem(new JiraIssueHistoryItem("comment"));

		JiraChangeLog changelog = new JiraChangeLog().addHistoryEntry(oldStatusTransition)
			.addHistoryEntry(latestStatusTransition)
			.addHistoryEntry(notAStatusTransition);
		
		Assertions.assertThat(changelog.getLatestStatusTransition()).isEqualTo(latestStatusTransition);
	}

}
