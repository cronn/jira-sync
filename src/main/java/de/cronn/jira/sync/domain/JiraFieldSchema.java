package de.cronn.jira.sync.domain;

import java.io.Serializable;

public class JiraFieldSchema implements Serializable {

	private static final long serialVersionUID = 2L;

	private String type;

	private String items;

	private String custom;

	public JiraFieldSchema() {
	}

	public JiraFieldSchema(String type, String items, String custom) {
		this.type = type;
		this.items = items;
		this.custom = custom;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setItems(String items) {
		this.items = items;
	}

	public String getItems() {
		return items;
	}

	public void setCustom(String custom) {
		this.custom = custom;
	}

	public String getCustom() {
		return custom;
	}
}
