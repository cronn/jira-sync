package de.cronn.jira.sync.dummy.changelog;

import java.beans.PropertyDescriptor;
import java.util.Set;

import de.cronn.jira.sync.SetUtils;
import de.cronn.jira.sync.domain.JiraIssueHistoryEntry;
import de.cronn.jira.sync.domain.JiraIssueHistoryItem;
import de.cronn.jira.sync.domain.JiraNamedBean;

public class CollectionChangeHistoryItemWriter implements HistoryItemWriter<Set<? extends JiraNamedBean>> {

	@Override
	public void add(JiraIssueHistoryEntry historyEntry, PropertyDescriptor propertyDescriptor, Set<? extends JiraNamedBean> oldValue, Set<? extends JiraNamedBean> newValue) {
		add(historyEntry, propertyDescriptor.getName(), oldValue, newValue);
	}

	private void add(JiraIssueHistoryEntry historyEntry, String propertyName, Set<? extends JiraNamedBean> oldValue, Set<? extends JiraNamedBean> newValue) {
		Set<JiraNamedBean> removedValues = SetUtils.difference(oldValue, newValue);
		Set<JiraNamedBean> addedValues = SetUtils.difference(newValue, oldValue);

		for (JiraNamedBean value : removedValues) {
			historyEntry.addItem(new JiraIssueHistoryItem(propertyName)
				.withFromString(value.getName())
				.withToString(null)
			);
		}

		for (JiraNamedBean value : addedValues) {
			historyEntry.addItem(new JiraIssueHistoryItem(propertyName)
				.withFromString(null)
				.withToString(value.getName())
			);
		}
	}

}
