package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraChangeLog implements Serializable {

	private static final long serialVersionUID = 1L;

	private int total;

	private List<JiraIssueHistoryEntry> histories;

	public JiraChangeLog() {
		this.histories = new ArrayList<>();
	}

	public JiraChangeLog(List<JiraIssueHistoryEntry> histories) {
		this.histories = histories;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<JiraIssueHistoryEntry> getHistories() {
		return histories;
	}

	public void setHistories(List<JiraIssueHistoryEntry> histories) {
		this.histories = histories;
	}

	@JsonIgnore
	public JiraChangeLog addHistoryEntry(JiraIssueHistoryEntry historyEntry) {
		getHistories().add(historyEntry);
		return this;
	}

	@JsonIgnore
	public JiraIssueHistoryEntry getLatestStatusTransition() {
		return histories.stream()
			.filter(historyEntry -> historyEntry.hasItemWithField(WellKnownJiraField.STATUS))
			.max(Comparator.comparing(JiraIssueHistoryEntry::getCreated))
			.orElse(null);
	}

}
