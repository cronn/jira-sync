package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import org.mockito.junit.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraComponent;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultComponentMapperTest {

	private static final JiraComponent SOURCE_COMPONENT_1 = new JiraComponent("10", "Source Component 1");
	private static final JiraComponent SOURCE_COMPONENT_2 = new JiraComponent("20", "Source Component 2");

	private static final JiraComponent TARGET_COMPONENT_1 = new JiraComponent("1", "Target Component 1");
	private static final JiraComponent TARGET_COMPONENT_2 = new JiraComponent("2", "Target Component 2");

	private static final String SOURCE_PROJECT = "SOURCE";
	private static final String TARGET_PROJECT = "TARGET";

	@InjectMocks
	private DefaultComponentMapper componentMapper;

	@Mock
	private JiraService jiraService;

	@Spy
	private final JiraProjectSync projectSync = new JiraProjectSync();

	@Before
	public void setUpProjectSyncConfig() {
		projectSync.setSourceProject(SOURCE_PROJECT);
		projectSync.setTargetProject(TARGET_PROJECT);

		Map<String, String> componentMapping = new LinkedHashMap<>();
		componentMapping.put(SOURCE_COMPONENT_1.getName(), TARGET_COMPONENT_1.getName());
		componentMapping.put(SOURCE_COMPONENT_2.getName(), TARGET_COMPONENT_2.getName());

		projectSync.setComponentMapping(componentMapping);

		when(jiraService.getComponents(TARGET_PROJECT)).thenReturn(Arrays.asList(TARGET_COMPONENT_1, TARGET_COMPONENT_2));
		when(jiraService.getComponents(SOURCE_PROJECT)).thenReturn(Arrays.asList(SOURCE_COMPONENT_1, SOURCE_COMPONENT_2));
	}

	@Test
	public void testMapSourceToTarget() throws Exception {
		List<JiraComponent> sourceComponents = Arrays.asList(SOURCE_COMPONENT_2, SOURCE_COMPONENT_1);

		Set<JiraComponent> targetComponents = componentMapper.mapSourceToTarget(jiraService, sourceComponents, projectSync);

		assertThat(targetComponents).containsExactly(TARGET_COMPONENT_2, TARGET_COMPONENT_1);

		verify(jiraService).getComponents(TARGET_PROJECT);
		verifyNoMoreInteractions(jiraService);
	}

	@Test
	public void testMapTargetToSource() throws Exception {
		List<JiraComponent> targetComponents = Arrays.asList(TARGET_COMPONENT_1, TARGET_COMPONENT_2);

		Set<JiraComponent> sourceComponents = componentMapper.mapTargetToSource(jiraService, targetComponents, projectSync);

		assertThat(sourceComponents).containsExactly(SOURCE_COMPONENT_1, SOURCE_COMPONENT_2);

		verify(jiraService).getComponents(SOURCE_PROJECT);
		verifyNoMoreInteractions(jiraService);
	}

}
