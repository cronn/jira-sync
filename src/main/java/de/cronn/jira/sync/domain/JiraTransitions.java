package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.List;

public class JiraTransitions implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<JiraTransition> transitions;

	public JiraTransitions() {
	}

	public JiraTransitions(List<JiraTransition> transitions) {
		this.transitions = transitions;
	}

	public List<JiraTransition> getTransitions() {
		return transitions;
	}

	public void setTransitions(List<JiraTransition> transitions) {
		this.transitions = transitions;
	}
}
