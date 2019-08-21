package de.cronn.jira.sync.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import de.cronn.jira.sync.config.JiraConnectionProperties;
import de.cronn.jira.sync.domain.JiraFilterResult;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraLoginRequest;
import de.cronn.jira.sync.domain.JiraLoginResponse;
import de.cronn.jira.sync.domain.JiraSearchResult;
import de.cronn.jira.sync.domain.WellKnownJiraField;

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
			.withRootCauseExactlyInstanceOf(NumberFormatException.class);
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

	@Test
	public void testGetIssuesByFilterId_empty() throws Exception {
		JiraSearchResult searchResult = new JiraSearchResult();
		searchResult.setTotal(0);
		searchResult.setIssues(Collections.emptyList());
		searchResult.setMaxResults(JiraServiceRestClient.DEFAULT_PAGE_SIZE);

		String filterId = prepareGetIssuesByFilterId(searchResult, 0);
		List<JiraIssue> issues = jiraServiceRestClient.getIssuesByFilterId(filterId, Collections.emptyList());
		assertThat(issues).isEmpty();
	}

	@Test
	public void testGetIssuesByFilterId_singlePage() throws Exception {
		JiraSearchResult searchResult = new JiraSearchResult();
		searchResult.setTotal(1);
		searchResult.setIssues(Collections.singletonList(new JiraIssue("123", "TEST-123")));
		searchResult.setMaxResults(JiraServiceRestClient.DEFAULT_PAGE_SIZE);

		String filterId = prepareGetIssuesByFilterId(searchResult, 0);
		List<JiraIssue> issues = jiraServiceRestClient.getIssuesByFilterId(filterId, Collections.emptyList());
		assertThat(issues).extracting(JiraIssue::getKey).containsExactly("TEST-123");
	}

	@Test
	public void testGetIssuesByFilterId_fullPage() throws Exception {
		JiraSearchResult searchResult = new JiraSearchResult();
		searchResult.setTotal(2);
		searchResult.setIssues(Arrays.asList(
			new JiraIssue("123", "TEST-123"),
			new JiraIssue("456", "TEST-456")
		));
		searchResult.setMaxResults(JiraServiceRestClient.DEFAULT_PAGE_SIZE);

		String filterId = prepareGetIssuesByFilterId(searchResult, 0);
		List<JiraIssue> issues = jiraServiceRestClient.getIssuesByFilterId(filterId, Collections.emptyList());
		assertThat(issues).extracting(JiraIssue::getKey)
			.containsExactly("TEST-123", "TEST-456");
	}

	@Test
	public void testGetIssuesByFilterId_twoFullPages() throws Exception {
		JiraSearchResult result1 = new JiraSearchResult();
		result1.setTotal(4);
		result1.setIssues(Arrays.asList(
			new JiraIssue("1", "TEST-1"),
			new JiraIssue("2", "TEST-2")
		));
		result1.setMaxResults(2);

		JiraSearchResult result2 = new JiraSearchResult();
		result2.setTotal(4);
		result2.setIssues(Arrays.asList(
			new JiraIssue("3", "TEST-3"),
			new JiraIssue("4", "TEST-4")
		));
		result2.setMaxResults(2);

		String filterId = prepareGetIssuesByFilterId(result1, 0);
		prepareGetIssuesByFilterId(result2, 2);

		List<JiraIssue> issues = jiraServiceRestClient.getIssuesByFilterId(filterId, Collections.emptyList(), 2);
		assertThat(issues).extracting(JiraIssue::getKey)
			.containsExactly("TEST-1", "TEST-2", "TEST-3", "TEST-4");
	}

	@Test
	public void testGetIssuesByFilterId_oneAndHalfPage() throws Exception {
		JiraSearchResult result1 = new JiraSearchResult();
		result1.setTotal(3);
		result1.setIssues(Arrays.asList(
			new JiraIssue("1", "TEST-1"),
			new JiraIssue("2", "TEST-2")
		));
		result1.setMaxResults(2);

		JiraSearchResult result2 = new JiraSearchResult();
		result2.setTotal(3);
		result2.setIssues(Collections.singletonList(
			new JiraIssue("3", "TEST-3")
		));
		result2.setMaxResults(2);

		String filterId = prepareGetIssuesByFilterId(result1, 0);
		prepareGetIssuesByFilterId(result2, 2);

		List<JiraIssue> issues = jiraServiceRestClient.getIssuesByFilterId(filterId, Collections.emptyList(), 2);
		assertThat(issues).extracting(JiraIssue::getKey)
			.containsExactly("TEST-1", "TEST-2", "TEST-3");
	}

	private String prepareGetIssuesByFilterId(JiraSearchResult searchResult, long startAt) {
		JiraConnectionProperties connectionProperties = validConnectionProperties();
		jiraServiceRestClient.login(connectionProperties, true);

		String filterId = "1234";
		String jql = "some JQL";
		JiraFilterResult filterResult = new JiraFilterResult();
		filterResult.setJql(jql);
		doReturn(filterResult).when(restTemplate).getForObject("http://localhost/jira/rest/api/2/filter/{id}", JiraFilterResult.class, filterId);

		String fieldsToFetch = Arrays.stream(WellKnownJiraField.values()).map(WellKnownJiraField::getFieldName).collect(Collectors.joining(","));

		doReturn(searchResult).when(restTemplate).getForObject("http://localhost/jira/rest/api/2/search?jql={jql}&fields={fieldsToFetch}&startAt={startAt}&maxResults={pageSize}",
			JiraSearchResult.class, filterResult.getJql(), fieldsToFetch, startAt, searchResult.getMaxResults());
		return filterId;
	}

	private static JiraConnectionProperties validConnectionProperties() {
		JiraConnectionProperties connectionProperties = new JiraConnectionProperties();
		connectionProperties.setUrl("http://localhost/jira");
		connectionProperties.setUsername("some-user");
		connectionProperties.setPassword("some-user");
		return connectionProperties;
	}

}
