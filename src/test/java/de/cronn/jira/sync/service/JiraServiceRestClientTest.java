package de.cronn.jira.sync.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import de.cronn.jira.sync.config.JiraConnectionProperties;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraLoginResponse;

@RunWith(MockitoJUnitRunner.class)
public class JiraServiceRestClientTest {

	@Mock
	private RestTemplateBuilder restTemplateBuilder;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private JiraServiceRestClient jiraServiceRestClient;

	@Before
	public void prepareMocks() {
		doReturn(restTemplateBuilder).when(restTemplateBuilder).errorHandler(any());
		doReturn(restTemplate).when(restTemplateBuilder).build();
	}

	@Test
	public void testLoginWithValidParameters() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		jiraServiceRestClient.login(connectionProperties, true);

		verify(restTemplate).postForObject(eq("http://localhost/jira/rest/auth/1/session"), any(JiraLoginRequest.class), eq(JiraLoginResponse.class));
		verifyNoMoreInteractions(restTemplate);
	}

	@Test
	public void testLoginWithMissingUrl() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		connectionProperties.setUrl(null);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> jiraServiceRestClient.login(connectionProperties, true))
			.withMessage("url is missing");
	}

	@Test
	public void testLoginWithInvalidUrlProtocol() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		connectionProperties.setUrl("unknown-scheme://foo-bar");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> jiraServiceRestClient.login(connectionProperties, true))
			.withMessage("Illegal URL: 'unknown-scheme://foo-bar'")
			.withCauseExactlyInstanceOf(MalformedURLException.class)
			.withStackTraceContaining("unknown protocol: unknown-scheme");
	}

	@Test
	public void testLoginWithInvalidUrl() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		connectionProperties.setUrl("http://foo:bar");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> jiraServiceRestClient.login(connectionProperties, true))
			.withMessage("Illegal URL: 'http://foo:bar'")
			.withCauseExactlyInstanceOf(MalformedURLException.class)
			.withRootCauseExactlyInstanceOf(NumberFormatException.class)
			.withStackTraceContaining("For input string: \"bar\"");
	}

	@Test
	public void testLoginWithMissingUsername() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		connectionProperties.setUsername(null);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> jiraServiceRestClient.login(connectionProperties, true))
			.withMessage("username is missing");
	}

	@Test
	public void testLoginWithMissingPassword() throws Exception {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		connectionProperties.setPassword(null);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> jiraServiceRestClient.login(connectionProperties, true))
			.withMessage("password is missing");
	}

	private static JiraConnectionProperties validConnectionProperties() {
		JiraConnectionProperties connectionProperties = new JiraConnectionProperties();
		connectionProperties.setUrl("http://localhost/jira");
		connectionProperties.setUsername("some-user");
		connectionProperties.setPassword("some-user");
		return connectionProperties;
	}

}