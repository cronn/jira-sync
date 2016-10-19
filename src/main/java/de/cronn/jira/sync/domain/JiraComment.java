package de.cronn.jira.sync.domain;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class JiraComment extends JiraIdResource {

	private static final long serialVersionUID = 1L;

	private JiraUser author;

	private String body;

	@JsonFormat(pattern = JiraResource.JIRA_DATE_FORMAT)
	private ZonedDateTime created;

	@JsonFormat(pattern = JiraResource.JIRA_DATE_FORMAT)
	private ZonedDateTime updated;

	public JiraComment() {
	}

	public JiraComment(String body) {
		this.body = body;
	}

	public JiraUser getAuthor() {
		return author;
	}

	public void setAuthor(JiraUser author) {
		this.author = author;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ZonedDateTime getCreated() {
		return created;
	}

	public void setCreated(ZonedDateTime created) {
		this.created = created;
	}

	public ZonedDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(ZonedDateTime updated) {
		this.updated = updated;
	}
}
