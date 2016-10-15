package de.cronn.jira.sync.dummy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthCredentials {

	private final String username;

	private final String password;

	public BasicAuthCredentials(String username, String password) {

		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String encodeBase64() {
		String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
}
