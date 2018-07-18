package de.cronn.jira.sync.domain;

import java.io.Serializable;

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
    	this.field = field.getName();
    }
    
    public static JiraIssueHistoryItem createStatusTransition(String from, String to) {
    	JiraIssueHistoryItem statusTransition = new JiraIssueHistoryItem(WellKnownJiraField.STATUS.getName());
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((fromString == null) ? 0 : fromString.hashCode());
		result = prime * result + ((toString == null) ? 0 : toString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JiraIssueHistoryItem other = (JiraIssueHistoryItem) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (fromString == null) {
			if (other.fromString != null)
				return false;
		} else if (!fromString.equals(other.fromString))
			return false;
		if (toString == null) {
			if (other.toString != null)
				return false;
		} else if (!toString.equals(other.toString))
			return false;
		return true;
	}
}
