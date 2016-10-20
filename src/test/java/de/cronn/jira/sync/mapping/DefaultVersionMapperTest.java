package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultVersionMapperTest {

	private static final JiraVersion SOURCE_VERSION_1 = new JiraVersion("10", "1.0");
	private static final JiraVersion SOURCE_VERSION_2 = new JiraVersion("20", "2.0");

	private static final JiraVersion TARGET_VERSION_1 = new JiraVersion("1", "Release 1");
	private static final JiraVersion TARGET_VERSION_2 = new JiraVersion("2", "Release 2");

	private static final String SOURCE_PROJECT = "SOURCE";
	private static final String TARGET_PROJECT = "TARGET";

	@InjectMocks
	private DefaultVersionMapper versionMapper;

	@Mock
	private JiraService jiraService;

	@Spy
	private JiraProjectSync projectSync = new JiraProjectSync();

	@Before
	public void setUpProjectSyncConfig() {
		projectSync.setSourceProject(SOURCE_PROJECT);
		projectSync.setTargetProject(TARGET_PROJECT);

		Map<String, String> versionMapping = new LinkedHashMap<>();
		versionMapping.put(SOURCE_VERSION_1.getName(), TARGET_VERSION_1.getName());
		versionMapping.put(SOURCE_VERSION_2.getName(), TARGET_VERSION_2.getName());

		projectSync.setVersionMapping(versionMapping);

		when(jiraService.getVersions(TARGET_PROJECT)).thenReturn(Arrays.asList(TARGET_VERSION_1, TARGET_VERSION_2));
		when(jiraService.getVersions(SOURCE_PROJECT)).thenReturn(Arrays.asList(SOURCE_VERSION_1, SOURCE_VERSION_2));
	}

	@Test
	public void testMapVersion_Empty() throws Exception {
		projectSync = new JiraProjectSync();

		Set<JiraVersion> versions = versionMapper.mapSourceToTarget(jiraService, null, projectSync);
		assertThat(versions).isEmpty();

		versions = versionMapper.mapSourceToTarget(jiraService, Collections.emptySet(), projectSync);
		assertThat(versions).isEmpty();

		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testMapVersion_SingleVersion() throws Exception {
		// given
		List<JiraVersion> versions = Collections.singletonList(SOURCE_VERSION_2);

		// when
		Set<JiraVersion> targetVersions = versionMapper.mapSourceToTarget(jiraService, versions, projectSync);

		// then
		assertThat(targetVersions).containsExactly(TARGET_VERSION_2);

		verify(jiraService).getVersions(TARGET_PROJECT);
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testMapVersionFromTargetToSource_SingleVersion() throws Exception {
		// given
		List<JiraVersion> versions = Collections.singletonList(TARGET_VERSION_2);

		// when
		Set<JiraVersion> targetVersions = versionMapper.mapTargetToSource(jiraService, versions, projectSync);

		// then
		assertThat(targetVersions).containsExactly(SOURCE_VERSION_2);

		verify(jiraService).getVersions(SOURCE_PROJECT);
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testMapVersion_MultipleVersion() throws Exception {
		// given
		List<JiraVersion> versions = Arrays.asList(SOURCE_VERSION_2, SOURCE_VERSION_1);

		// when
		Set<JiraVersion> targetVersions = versionMapper.mapSourceToTarget(jiraService, versions, projectSync);

		// then
		assertThat(targetVersions).containsExactly(TARGET_VERSION_2, TARGET_VERSION_1);

		verify(jiraService, atLeast(1)).getVersions(TARGET_PROJECT);
		verifyNoMoreInteractions(jiraService);
	}

}