package de.cronn.jira.sync.dummy.changelog;

import java.beans.PropertyDescriptor;

import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;

public interface HistoryItemWriter<T> {
	void add(JiraIssueHistoryEntry historyEntry, PropertyDescriptor propertyDescriptor, T oldValue, T newValue);
}
