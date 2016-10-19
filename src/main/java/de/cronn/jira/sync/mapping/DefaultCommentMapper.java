package de.cronn.jira.sync.mapping;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultCommentMapper implements CommentMapper {

	private static final String TAB_PANEL_PAGE_ID = "com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel";

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

	private Clock clock;

	@Autowired
	public void setClock(Clock clock) {
		this.clock = clock;
	}

	@Override
	public String map(JiraIssue sourceIssue, JiraComment comment, JiraService jiraSource, boolean behindTime) {
		String originalCommentId = getOriginalCommentId(comment);
		String author = getAuthorDisplayName(comment);
		String originalDateString = getOriginalDateString(comment);
		String sourceKey = getIssueKey(sourceIssue);
		return "{panel:title=" + author + " - " + originalDateString + "|" + getPanelColors(behindTime) + "}\n" +
			comment.getBody() + "\n" +
			"~??[comment " + originalCommentId + "|" + buildCommentLink(jiraSource, originalCommentId, sourceKey) + "]??~\n" +
			(behindTime ? "~(!) This comment was added behind time. The order of comments might not represent the real order.~\n" : "") +
			"{panel}";
	}

	private String getPanelColors(boolean outOfOrder) {
		if (outOfOrder) {
			return "titleBGColor=#CCC|bgColor=#DDD";
		} else {
			return "titleBGColor=#DDD|bgColor=#EEE";
		}
	}

	@Override
	public boolean isMapped(JiraComment commentInSource, String commentTextInTarget) {
		String sourceCommentId = commentInSource.getId();
		Assert.hasText(sourceCommentId);
		return commentTextInTarget.startsWith("{panel:title=") && commentTextInTarget.contains("focusedCommentId=" + sourceCommentId + "&");
	}

	private String buildCommentLink(JiraService jiraSource, String originalCommentId, String sourceKey) {
		String baseUrl = getBaseUrl(jiraSource);
		return baseUrl + "browse/" + sourceKey + "?focusedCommentId=" + originalCommentId + "&page=" + TAB_PANEL_PAGE_ID + "#comment-" + originalCommentId;
	}

	private String getBaseUrl(JiraService jiraSource) {
		String baseUrl = jiraSource.getServerInfo().getBaseUrl();
		return baseUrl + (baseUrl.endsWith("/") ? "" : "/");
	}

	private String getIssueKey(JiraIssue issue) {
		String key = issue.getKey();
		Assert.notNull(key);
		return key;
	}

	private String getOriginalDateString(JiraComment comment) {
		ZonedDateTime created = comment.getCreated();
		Assert.notNull(created);
		return DATE_TIME_FORMATTER.format(created.withZoneSameInstant(clock.getZone()));
	}

	private String getOriginalCommentId(JiraComment comment) {
		String originalCommentId = comment.getId();
		Assert.notNull(originalCommentId);
		return originalCommentId;
	}

	private String getAuthorDisplayName(JiraComment comment) {
		String author = comment.getAuthor().getDisplayName();
		Assert.notNull(author);
		return author;
	}

}
