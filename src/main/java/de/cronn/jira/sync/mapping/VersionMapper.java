package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

public final class VersionMapper {

	private static final Logger log = LoggerFactory.getLogger(VersionMapper.class);

	private VersionMapper() {
	}

	public static Set<JiraVersion> mapVersions(JiraService jiraTarget, Collection<JiraVersion> sourceVersions, JiraProjectSync projectSync) {
		if (projectSync.getVersionMapping() == null) {
			log.warn("no version mapping configured for {}", projectSync);
			return Collections.emptySet();
		}

		if (CollectionUtils.isEmpty(sourceVersions)) {
			return Collections.emptySet();
		}

		List<JiraVersion> projectVersions = jiraTarget.getVersions(projectSync.getTargetProject());

		Set<JiraVersion> targetVersions = new LinkedHashSet<>();
		for (JiraVersion sourceVersion : sourceVersions) {
			String sourceVersionName = sourceVersion.getName();
			if (sourceVersionName == null) {
				throw new JiraSyncException("sourceVersionName not set: " + sourceVersion);
			}

			String targetVersionName = projectSync.getVersionMapping().get(sourceVersionName);
			if (targetVersionName == null) {
				log.warn("no mapping defined for {}", sourceVersion);
				continue;
			}

			JiraVersion targetVersion = projectVersions.stream()
				.filter(priority -> Objects.equals(priority.getName(), targetVersionName))
				.findFirst()
				.orElse(null);

			if (targetVersion == null) {
				log.warn("target priority '{}' not found", targetVersionName);
				continue;
			}

			Assert.notNull(targetVersion);
			log.trace("version: {}  -->  {}", sourceVersion, targetVersion);
			targetVersions.add(targetVersion);
		}

		return targetVersions;
	}
}
