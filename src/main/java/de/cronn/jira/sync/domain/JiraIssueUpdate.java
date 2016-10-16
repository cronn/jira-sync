package de.cronn.jira.sync.domain;

public class JiraIssueUpdate {

	private JiraFieldsUpdate fields;

	private JiraTransition transition;

	public JiraIssueUpdate() {
	}

	public JiraIssueUpdate(JiraFieldsUpdate fields) {
		this.fields = fields;
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

	public boolean isEmpty() {
		return fields == null && transition == null;
	}

	public JiraFieldsUpdate getOrCreateFields() {
		JiraFieldsUpdate fields = getFields();
		if (fields == null) {
			setFields(new JiraFieldsUpdate());
		}
		return getFields();
	}
}
