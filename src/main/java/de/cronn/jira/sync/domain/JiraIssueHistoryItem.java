package de.cronn.jira.sync.domain;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

	public String getToString() {
		return toString;
	}

	public void setToString(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("field", getField())
			.append("fromString", getFromString())
			.append("toString", getToString())
			.toString();
	}

}
