package de.cronn.jira.sync.mapping;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraProject;

@Component
public class DefaultIssueTypeMapper implements IssueTypeMapper {

	private static final Logger log = LoggerFactory.getLogger(DefaultIssueTypeMapper.class);

	@Override
	public JiraIssueType mapIssueType(JiraIssue sourceIssue, JiraSyncConfig syncConfig, JiraProjectSync projectSync, JiraProject targetProject) {
		JiraIssueType issueType = map(sourceIssue, syncConfig, targetProject);
		if (issueType == null) {
			return getFallbackIssueType(projectSync, targetProject);
		}
		return issueType;
	}

	private static JiraIssueType map(JiraIssue sourceIssue, JiraSyncConfig syncConfig, JiraProject targetProject) {
		if (syncConfig.getIssueTypeMapping() == null) {
			log.warn("no issueTypeMapping configured");
			return null;
		}

		String sourceIssueTypeName = getSourceIssueType(sourceIssue);
		if (sourceIssueTypeName == null) {
			return null;
		}

		JiraIssueType targetIssueType = getTargetIssueType(syncConfig, targetProject, sourceIssueTypeName);
		if (targetIssueType == null) {
			return null;
		}

		log.debug("issueType: {}  -->  {}", sourceIssueTypeName, targetIssueType);
		return targetIssueType;
	}

	private static JiraIssueType getTargetIssueType(JiraSyncConfig syncConfig, JiraProject targetProject, String sourceIssueTypeName) {
		String targetIssueTypeName = syncConfig.getIssueTypeMapping().get(sourceIssueTypeName);
		if (targetIssueTypeName == null) {
			log.warn("no mapping defined for {}", sourceIssueTypeName);
			return null;
		}

		JiraIssueType targetIssueType = getIssueTypeByName(targetIssueTypeName, targetProject);

		if (targetIssueType == null) {
			log.warn("target issue type '{}' not found", targetIssueTypeName);
			return null;
		}
		return targetIssueType;
	}

	private static String getSourceIssueType(JiraIssue sourceIssue) {
		JiraIssueType sourceIssueType = sourceIssue.getFields().getIssuetype();
		if (sourceIssueType == null) {
			log.warn("{} has no issueType", sourceIssue);
			return null;
		}

		String sourceIssueTypeName = sourceIssueType.getName();
		Assert.notNull(sourceIssueTypeName, "sourceIssueTypeName not set for: " + sourceIssue);
		return sourceIssueTypeName;
	}

	private static JiraIssueType getFallbackIssueType(JiraProjectSync projectSync, JiraProject targetProject) {
		String targetIssueFallbackType = projectSync.getTargetIssueFallbackType();
		if (targetIssueFallbackType == null) {
			throw new JiraSyncException("TargetIssueFallbackType must be configured");
		}

		JiraIssueType issueType = getIssueTypeByName(targetIssueFallbackType, targetProject);
		if (issueType == null) {
			throw new JiraSyncException("TargetIssueFallbackType " + targetIssueFallbackType + " not found");
		}

		return issueType;
	}

	private static JiraIssueType getIssueTypeByName(String targetIssueTypeName, JiraProject targetProject) {
		List<JiraIssueType> projectIssueTypes = targetProject.getIssueTypes();
		Assert.notNull(projectIssueTypes, "Got no issue types for " + targetProject);
		return projectIssueTypes.stream()
			.filter(priority -> Objects.equals(priority.getName(), targetIssueTypeName))
			.findFirst()
			.orElse(null);
	}
}
