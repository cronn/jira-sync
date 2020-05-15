package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraIssue extends JiraIdResource {

	private static final long serialVersionUID = 1L;

	private String key;

	private JiraIssueFields fields;

	private JiraChangeLog changelog;

	public JiraIssue() {
	}

	public JiraIssue(JiraProject project) {
		this.fields = new JiraIssueFields(project);
	}

	public JiraIssue(String id, String key) {
		super(id);
		this.key = key;
	}

	public JiraIssue(String id, String key, String summary, JiraIssueStatus status) {
		this(id, key);
		fields = new JiraIssueFields();
		fields.setSummary(summary);
		fields.setStatus(status);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public JiraIssueFields getFields() {
		return fields;
	}

	public void setFields(JiraIssueFields fields) {
		this.fields = fields;
	}

	@JsonIgnore
	public JiraIssue withFields(JiraIssueFields fields) {
		setFields(fields);
		return this;
	}

	public JiraChangeLog getChangelog() {
		return changelog;
	}

	public void setChangelog(JiraChangeLog changelog) {
		this.changelog = changelog;
	}

	@JsonIgnore
	public JiraIssueFields getOrCreateFields() {
		if (fields == null) {
			fields = new JiraIssueFields();
		}
		return fields;
	}

	@JsonIgnore
	public JiraChangeLog getOrCreateChangeLog() {
		if (changelog == null) {
			changelog = new JiraChangeLog();
		}
		return changelog;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("id", getId())
			.append("key", key)
			.toString();
	}
}
