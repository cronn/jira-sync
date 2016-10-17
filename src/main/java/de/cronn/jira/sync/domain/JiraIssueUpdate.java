package de.cronn.jira.sync.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraIssueUpdate {

	private JiraFieldsUpdate fields;

	private JiraTransition transition;

	public JiraIssueUpdate() {
	}

	public JiraFieldsUpdate getFields() {
		return fields;
	}

	public void setFields(JiraFieldsUpdate fields) {
		this.fields = fields;
	}

	public JiraTransition getTransition() {
		return transition;
	}

	public void setTransition(JiraTransition transition) {
		this.transition = transition;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return fields == null && transition == null;
	}

	@JsonIgnore
	public JiraFieldsUpdate getOrCreateFields() {
		JiraFieldsUpdate fields = getFields();
		if (fields == null) {
			setFields(new JiraFieldsUpdate());
		}
		return getFields();
	}
}
