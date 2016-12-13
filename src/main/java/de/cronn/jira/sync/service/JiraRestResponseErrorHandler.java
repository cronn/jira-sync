package de.cronn.jira.sync.service;

import java.io.IOException;
import java.net.URL;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import de.cronn.jira.sync.JiraSyncException;

public class JiraRestResponseErrorHandler extends DefaultResponseErrorHandler {

	private final URL jiraUrl;

	public JiraRestResponseErrorHandler(URL jiraUrl) {
		this.jiraUrl = jiraUrl;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		try {
			super.handleError(response);
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			String responseBodyAsString = e.getResponseBodyAsString();
			String message = e.getStatusCode().getReasonPhrase() + ": " + responseBodyAsString;
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				throw new JiraResourceNotFoundException(jiraUrl, message);
			} else {
				throw new JiraSyncException(jiraUrl, message);
			}
		}
	}
}
