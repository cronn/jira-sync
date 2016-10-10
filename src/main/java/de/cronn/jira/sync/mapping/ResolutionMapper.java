package de.cronn.jira.sync.mapping;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraResolution;
import de.cronn.jira.sync.service.JiraService;

public final class ResolutionMapper {

	private static final Logger log = LoggerFactory.getLogger(ResolutionMapper.class);

	private ResolutionMapper() {
	}

	public static JiraResolution mapResolution(JiraService jiraService, JiraIssue jiraIssue, JiraSyncConfig syncConfig) {
		if (syncConfig.getPriorityMapping() == null) {
			return null;
		}

		String sourceResolutionName = getSourceResolutionName(jiraIssue);
		if (sourceResolutionName == null) {
			return null;
		}

		JiraResolution targetResolution = getTargetResolution(jiraService, syncConfig, sourceResolutionName);
		if (targetResolution == null) {
			return null;
		}

		log.trace("resolution: {}  -->  {}", sourceResolutionName, targetResolution);
		return targetResolution;
	}

	private static JiraResolution getTargetResolution(JiraService jiraService, JiraSyncConfig syncConfig, String sourceResolutionName) {
		String targetResolutionName = syncConfig.getResolutionMapping().get(sourceResolutionName);
		if (targetResolutionName == null) {
			log.warn("no mapping defined for {}", sourceResolutionName);
			return null;
		}

		JiraResolution targetResolution = jiraService.getResolutions().stream()
			.filter(priority -> Objects.equals(priority.getName(), targetResolutionName))
			.findFirst()
			.orElse(null);

		if (targetResolution == null) {
			log.warn("target resolution '{}' not found", targetResolutionName);
			return null;
		}
		return targetResolution;
	}

	private static String getSourceResolutionName(JiraIssue jiraIssue) {
		JiraResolution sourceResolution = jiraIssue.getFields().getResolution();
		if (sourceResolution == null) {
			return null;
		}

		String sourceResolutionName = sourceResolution.getName();
		Assert.notNull(sourceResolutionName, "sourceResolutionName not set for: " + jiraIssue);
		return sourceResolutionName;
	}
}
