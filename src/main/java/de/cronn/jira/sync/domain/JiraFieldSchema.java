package de.cronn.jira.sync.domain;

import java.io.Serializable;

public class JiraFieldSchema implements Serializable {

	private static final long serialVersionUID = 1L;

	private String type;

	private String items;

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
}
