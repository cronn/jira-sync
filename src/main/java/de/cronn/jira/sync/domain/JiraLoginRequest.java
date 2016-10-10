package de.cronn.jira.sync.domain;

public class JiraLoginRequest {

	private final String username;
	private final String password;

	public JiraLoginRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

}
