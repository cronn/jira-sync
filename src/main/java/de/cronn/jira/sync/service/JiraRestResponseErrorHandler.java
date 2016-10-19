package de.cronn.jira.sync.service;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;

import de.cronn.jira.sync.JiraSyncException;

public class JiraRestResponseErrorHandler extends DefaultResponseErrorHandler {

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		try {
			super.handleError(response);
		} catch (HttpClientErrorException e) {
			String responseBodyAsString = e.getResponseBodyAsString();
			String message = e.getStatusCode().getReasonPhrase() + ": " + responseBodyAsString;
			throw new JiraSyncException(message);
		}
	}
}
