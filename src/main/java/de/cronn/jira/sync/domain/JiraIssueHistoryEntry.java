package de.cronn.jira.sync.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JiraIssueHistoryEntry extends JiraIdResource {

	private static final long serialVersionUID = 1L;

	private ZonedDateTime created;
	
	private List<JiraIssueHistoryItem> items = new ArrayList<>();
	
	public JiraIssueHistoryEntry() {

	}
	
	public JiraIssueHistoryEntry(String id) {
		super(id);
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}
	
	public JiraIssueHistoryEntry withCreated(ZonedDateTime created) {
		this.created = created;
		return this;
	}

	public List<JiraIssueHistoryItem> getItems() {
		return items;
	}

	public void setItems(List<JiraIssueHistoryItem> items) {
		this.items = items;
	}
	
	public boolean hasItemWithField(String fieldName) {
		return items.stream().anyMatch(item -> Objects.equals(item.getField(), fieldName));
	}

	public JiraIssueHistoryEntry addItem(JiraIssueHistoryItem item) {
		this.items.add(item);
		return this;
	}
	
}
