package de.cronn.jira.sync.mapping;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.domain.JiraIssue;

@Component
public class DefaultSummaryMapper implements SummaryMapper {

	@Override
	public String mapSummary(JiraIssue sourceIssue) {
		String key = sourceIssue.getKey();
		String summary = sourceIssue.getFields().getSummary();
		Assert.notNull(key, "key must not be null");
		Assert.notNull(summary, "summary must not be null");
		return key + ": " + summary;
	}
}
