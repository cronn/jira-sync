package de.cronn.jira.sync;

public class JiraSyncException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JiraSyncException(String jiraUrl, String message) {
		this(buildMessage(jiraUrl, message));
	}

	private static String buildMessage(String jiraUrl, String message) {
		return String.format("[%s] %s", jiraUrl, message);
	}

	public JiraSyncException(String message) {
		super(message);
	}

	public JiraSyncException(String message, Throwable cause) {
		super(message, cause);
	}
}
