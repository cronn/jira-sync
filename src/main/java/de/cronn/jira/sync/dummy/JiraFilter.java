package de.cronn.jira.sync.dummy;

import de.cronn.jira.sync.domain.JiraIssue;

public interface JiraFilter {

	boolean shouldInclude(JiraIssue issue);

}
