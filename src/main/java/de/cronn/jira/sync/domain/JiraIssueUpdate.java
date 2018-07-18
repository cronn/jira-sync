package de.cronn.jira.sync.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraIssueUpdate implements Serializable {

	private static final long serialVersionUID = 1L;

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
	
	@JsonIgnore
	public JiraIssueUpdate withFields(JiraFieldsUpdate fields) {
		setFields(fields);
		return this;
	}

	public JiraTransition getTransition() {
		return transition;
	}

	public void setTransition(JiraTransition transition) {
		this.transition = transition;
	}
	
	@JsonIgnore
	public JiraIssueUpdate withTransition(JiraTransition jiraTransition) {
		setTransition(jiraTransition);
		return this;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return fields == null && transition == null;
	}

	@JsonIgnore
	public JiraFieldsUpdate getOrCreateFields() {
		if (this.fields == null) {
			this.fields = new JiraFieldsUpdate();
		}
		return this.fields;
	}
}
