package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JiraComments implements Serializable {

	private static final long serialVersionUID = 1L;

	private int startAt;
	private int maxResults;
	private int total;
	private List<JiraComment> comments = new ArrayList<>();

	public int getStartAt() {
		return startAt;
	}

	public void setStartAt(int startAt) {
		this.startAt = startAt;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<JiraComment> getComments() {
		return comments;
	}

	public void setComments(List<JiraComment> comments) {
		this.comments = comments;
	}

	public void addComment(JiraComment comment) {
		this.comments.add(comment);
	}
}
