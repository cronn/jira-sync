package de.cronn.jira.sync.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class JiraIssueUpdate {

	private Map<String, Object> fields;

	private JiraTransition transition;

	public JiraIssueUpdate() {
	}

	public JiraIssueUpdate(Map<String, Object> fields) {
		this.fields = new LinkedHashMap<>(fields);
	}

	public Map<String, Object> getFields() {
		return fields;
	}

	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}

	public JiraTransition getTransition() {
		return transition;
	}

	public void setTransition(JiraTransition transition) {
		this.transition = transition;
	}

	public boolean isEmpty() {
		return CollectionUtils.isEmpty(fields) && transition == null;
	}

	public void putFieldUpdate(JiraField field, Object newValue) {
		if (fields == null) {
			fields = new LinkedHashMap<>();
		}
		fields.put(field.getName(), newValue);
	}
}
