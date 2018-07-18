package de.cronn.jira.sync.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraIssueHistoryEntry extends JiraIdResource {

	private static final long serialVersionUID = 1L;
	
	@JsonFormat(pattern = JIRA_DATE_FORMAT)
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
	
	@JsonIgnore
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
	
	@JsonIgnore
	public JiraIssueHistoryEntry addItem(JiraIssueHistoryItem item) {
		this.items.add(item);
		return this;
	}
	
	@JsonIgnore
	public boolean hasItemWithField(String fieldName) {
		return items.stream().anyMatch(item -> Objects.equals(item.getField(), fieldName));
	}
}
