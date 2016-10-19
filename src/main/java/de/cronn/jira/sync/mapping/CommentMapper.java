package de.cronn.jira.sync.mapping;

import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

public interface CommentMapper {

	String map(JiraIssue sourceIssue, JiraComment comment, JiraService jiraSource, boolean behindTime);

	boolean isMapped(JiraComment commentInSource, String commentInTargetBody);

}
