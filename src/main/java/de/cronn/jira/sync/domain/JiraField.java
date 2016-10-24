package de.cronn.jira.sync.domain;

public class JiraField extends JiraIdResource {

	private static final long serialVersionUID = 1L;

	private String name;

	public JiraField() {
	}

	public JiraField(String id, String name) {
		super(id);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}