package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraProject;

public class DefaultIssueTypeMapperTest {

	private static final JiraIssueType SOURCE_ISSUE_TYPE_IMPROVEMENT = new JiraIssueType("100", "Improvement");

	private static final JiraIssueType TARGET_ISSUE_TYPE_TASK = new JiraIssueType("1", "Task");
	private static final JiraIssueType TARGET_ISSUE_TYPE_BUG = new JiraIssueType("2", "Bug");
	private static final JiraIssueType TARGET_ISSUE_TYPE_NEW_FEATURE = new JiraIssueType("3", "New Feature");

	private JiraIssue sourceIssue;
	private JiraSyncConfig syncConfig;
	private JiraProjectSync projectSync;
	private JiraProject targetProject;

	private DefaultIssueTypeMapper issueTypeMapper;

	@Before
	public void setUp() {
		JiraProject sourceProject = new JiraProject("1", "SOURCE");
		sourceIssue = new JiraIssue(sourceProject);
		syncConfig = new JiraSyncConfig();
		projectSync = new JiraProjectSync();
		targetProject = new JiraProject("100", "TARGET");

		targetProject.setIssueTypes(Arrays.asList(
			TARGET_ISSUE_TYPE_TASK,
			TARGET_ISSUE_TYPE_BUG,
			TARGET_ISSUE_TYPE_NEW_FEATURE
		));

		Map<String, String> issueTypeMapping = new LinkedHashMap<>();
		issueTypeMapping.put(SOURCE_ISSUE_TYPE_IMPROVEMENT.getName(), TARGET_ISSUE_TYPE_NEW_FEATURE.getName());
		syncConfig.setIssueTypeMapping(issueTypeMapping);

		projectSync.setTargetIssueTypeFallback(TARGET_ISSUE_TYPE_TASK.getName());

		issueTypeMapper = new DefaultIssueTypeMapper();
	}

	@Test
	public void testMap_HappyPath() throws Exception {
		// given

		sourceIssue.getFields().setIssuetype(SOURCE_ISSUE_TYPE_IMPROVEMENT);

		// when
		JiraIssueType mappedIssueType = issueTypeMapper.mapIssueType(sourceIssue, syncConfig, projectSync, targetProject);

		// then
		assertThat(mappedIssueType).isSameAs(TARGET_ISSUE_TYPE_NEW_FEATURE);
	}


	@Test
	public void testMap_NoConfiguration_NoFallbackIssueTypeConfigured() throws Exception {
		projectSync.setTargetIssueTypeFallback(null);

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> issueTypeMapper.mapIssueType(sourceIssue, syncConfig, projectSync, targetProject))
			.withMessage("targetIssueTypeFallback must be configured");
	}

	@Test
	public void testMap_NoConfiguration_UsesFallbackIssueType() throws Exception {
		// given

		// when
		JiraIssueType issueType = issueTypeMapper.mapIssueType(sourceIssue, syncConfig, projectSync, targetProject);

		// then
		assertThat(issueType).isSameAs(TARGET_ISSUE_TYPE_TASK);
	}

	@Test
	public void testMap_NoConfiguration_UnknownFallbackIssueType() throws Exception {
		projectSync.setTargetIssueTypeFallback("Unknown");

		assertThatExceptionOfType(JiraSyncException.class)
			.isThrownBy(() -> issueTypeMapper.mapIssueType(sourceIssue, syncConfig, projectSync, targetProject))
			.withMessage("targetIssueTypeFallback Unknown not found");
	}

}