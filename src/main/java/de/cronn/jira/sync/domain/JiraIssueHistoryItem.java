package de.cronn.jira.sync.domain;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JiraIssueHistoryItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private String field;

    private String fromString;

    private String toString;

    public JiraIssueHistoryItem() {

    }

    public JiraIssueHistoryItem(String field) {
    	this.field = field;
    }

    public JiraIssueHistoryItem(WellKnownJiraField field) {
    	this.field = field.getFieldName();
    }

    public static JiraIssueHistoryItem createStatusTransition(String from, String to) {
    	JiraIssueHistoryItem statusTransition = new JiraIssueHistoryItem(WellKnownJiraField.STATUS.getFieldName());
    	statusTransition.setFromString(from);
    	statusTransition.setToString(to);
    	return statusTransition;
    }

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getFromString() {
		return fromString;
	}

	public void setFromString(String fromString) {
		this.fromString = fromString;
	}

	@JsonIgnore
	public JiraIssueHistoryItem withFromString(String from) {
		setFromString(from);
		return this;
	}

	public String getToString() {
		return toString;
	}

	public void setToString(String toString) {
		this.toString = toString;
	}

	@JsonIgnore
	public JiraIssueHistoryItem withToString(String to) {
		setToString(to);
		return this;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("field", getField())
			.append("fromString", getFromString())
			.append("toString", getToString())
			.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JiraIssueHistoryItem that = (JiraIssueHistoryItem) o;
		return Objects.equals(getField(), that.getField())
			&& Objects.equals(getFromString(), that.getFromString())
			&& Objects.equals(getToString(), that.getToString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getField(), getFromString(), getToString());
	}
}
