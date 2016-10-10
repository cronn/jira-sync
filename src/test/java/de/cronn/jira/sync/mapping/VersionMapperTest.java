package de.cronn.jira.sync.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class VersionMapperTest {

	private static final JiraVersion SOURCE_VERSION_1 = new JiraVersion("10", "1.0");
	private static final JiraVersion SOURCE_VERSION_2 = new JiraVersion("20", "2.0");

	private static final JiraVersion TARGET_VERSION_1 = new JiraVersion("1", "Release 1");
	private static final JiraVersion TARGET_VERSION_2 = new JiraVersion("2", "Release 2");

	private static final String TARGET_PROJECT = "TARGET";

	@Mock
	private JiraService jiraTarget;

	private JiraProjectSync projectConfig;

	@Before
	public void setUpProjectSyncConfig() {
		projectConfig = new JiraProjectSync();
		projectConfig.setTargetProject(TARGET_PROJECT);

		Map<String, String> versionMapping = new LinkedHashMap<>();
		versionMapping.put(SOURCE_VERSION_1.getName(), TARGET_VERSION_1.getName());
		versionMapping.put(SOURCE_VERSION_2.getName(), TARGET_VERSION_2.getName());

		projectConfig.setVersionMapping(versionMapping);

		when(jiraTarget.getVersions(TARGET_PROJECT)).thenReturn(Arrays.asList(TARGET_VERSION_1, TARGET_VERSION_2));
	}

	@Test
	public void testMapVersion_Empty() throws Exception {
		JiraProjectSync projectConfig = new JiraProjectSync();

		Set<JiraVersion> versions = VersionMapper.mapVersions(jiraTarget, null, projectConfig);
		assertThat(versions, empty());

		versions = VersionMapper.mapVersions(jiraTarget, Collections.emptySet(), projectConfig);
		assertThat(versions, empty());

		verifyNoMoreInteractions(jiraTarget);
	}

	@Test
	public void testMapVersion_SingleVersion() throws Exception {
		// given
		List<JiraVersion> versions = Collections.singletonList(SOURCE_VERSION_2);

		// when
		Set<JiraVersion> targetVersions = VersionMapper.mapVersions(jiraTarget, versions, projectConfig);

		// then
		assertThat(targetVersions, contains(TARGET_VERSION_2));

		verify(jiraTarget).getVersions(TARGET_PROJECT);
		verifyNoMoreInteractions(jiraTarget);
	}

	@Test
	public void testMapVersion_MultipleVersion() throws Exception {
		// given
		List<JiraVersion> versions = Arrays.asList(SOURCE_VERSION_2, SOURCE_VERSION_1);

		// when
		Set<JiraVersion> targetVersions = VersionMapper.mapVersions(jiraTarget, versions, projectConfig);

		// then
		assertThat(targetVersions, contains(TARGET_VERSION_2, TARGET_VERSION_1));

		verify(jiraTarget).getVersions(TARGET_PROJECT);
		verifyNoMoreInteractions(jiraTarget);
	}

}