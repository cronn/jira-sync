package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.service.JiraService;


@RunWith(MockitoJUnitRunner.class)
public class DefaultResolutionMapperTest {

	@Mock
	private JiraService jiraService;

	@Mock
	private JiraSyncConfig syncConfig;

	@InjectMocks
	private DefaultResolutionMapper resolutionMapper;

	@Test
	public void testMapNull() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("1", "KEY-1");
		issue.getOrCreateFields().setResolution(null);

		// when
		JiraResolution mappedResolution = resolutionMapper.mapResolution(jiraService, issue);

		// then
		assertThat(mappedResolution).isNull();

		verifyNoMoreInteractions(jiraService, syncConfig);
	}

	@Test
	public void testMapToUnmappedTargetResolution() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("1", "KEY-1");
		issue.getOrCreateFields().setResolution(new JiraResolution("1", "source resolution"));

		when(syncConfig.getResolutionMapping()).thenReturn(Collections.emptyMap());

		// when
		JiraResolution mappedResolution = resolutionMapper.mapResolution(jiraService, issue);

		// then
		assertThat(mappedResolution).isNull();

		verify(syncConfig).getResolutionMapping();
		verifyNoMoreInteractions(jiraService, syncConfig);
	}

	@Test
	public void testMapToUnknownTargetResolution() throws Exception {
		// given
		JiraIssue issue = new JiraIssue("1", "KEY-1");
		issue.getOrCreateFields().setResolution(new JiraResolution("1", "source resolution"));

		when(syncConfig.getResolutionMapping()).thenReturn(Collections.singletonMap("source resolution", "target resolution"));
		when(jiraService.getResolutions()).thenReturn(Collections.emptyList());

		// when
		JiraResolution mappedResolution = resolutionMapper.mapResolution(jiraService, issue);

		// then
		assertThat(mappedResolution).isNull();

		verify(syncConfig).getResolutionMapping();
		verify(jiraService).getResolutions();
		verifyNoMoreInteractions(jiraService, syncConfig);
	}

}