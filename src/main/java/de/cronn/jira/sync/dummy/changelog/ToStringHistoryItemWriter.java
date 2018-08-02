package de.cronn.jira.sync.dummy.changelog;

import java.beans.PropertyDescriptor;
import java.util.function.Function;

import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueHistoryItem;

public class ToStringHistoryItemWriter<T> implements HistoryItemWriter<T> {
	private final Function<T, String> toStringMapper;

	public ToStringHistoryItemWriter(Function<T, String> toStringMapper) {
		this.toStringMapper = toStringMapper;
	}

	@Override
	public void add(JiraIssueHistoryEntry historyEntry, PropertyDescriptor propertyDescriptor, T oldValue, T newValue) {
		historyEntry.addItem(new JiraIssueHistoryItem(propertyDescriptor.getName())
			.withFromString(toStringMapper.apply(oldValue))
			.withToString(toStringMapper.apply(newValue)));
	}
}
