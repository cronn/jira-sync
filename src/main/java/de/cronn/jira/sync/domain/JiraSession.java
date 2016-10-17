package de.cronn.jira.sync.domain;

import java.io.Serializable;

public class JiraSession implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
