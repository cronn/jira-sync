package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.MapUtils;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultVersionMapper implements VersionMapper {

	private static final Logger log = LoggerFactory.getLogger(DefaultVersionMapper.class);

	@Override
	public Set<JiraVersion> mapSourceToTarget(JiraService jiraService, Collection<JiraVersion> versionsToMap, JiraProjectSync projectSync) {
		Map<String, String> versionMapping = projectSync.getVersionMapping();
		String project = projectSync.getTargetProject();
		Set<String> versionsToIgnore = projectSync.getVersionsToIgnore();
		return mapVersions(jiraService, versionsToMap, versionMapping, versionsToIgnore, project);
	}

	@Override
	public Set<JiraVersion> mapTargetToSource(JiraService jiraService, Collection<JiraVersion> versionsToMap, JiraProjectSync projectSync) {
		Map<String, String> versionMapping = projectSync.getVersionMapping();
		Assert.notNull(versionMapping, "No version mapping configured for " + projectSync);
		Map<String, String> inverseVersionMapping = MapUtils.calculateInverseMapping(versionMapping);
		String project = projectSync.getSourceProject();
		return mapVersions(jiraService, versionsToMap, inverseVersionMapping, Collections.emptySet(), project);
	}

	private Set<JiraVersion> mapVersions(JiraService jiraService, Collection<JiraVersion> versionsToMap, Map<String, String> versionMapping, Set<String> versionsToIgnore, String projectKey) {
		if (CollectionUtils.isEmpty(versionMapping)) {
			log.warn("no version mapping configured for project '{}'", projectKey);
			return null;
		}

		if (CollectionUtils.isEmpty(versionsToMap)) {
			return Collections.emptySet();
		}

		Set<JiraVersion> mappedVersions = new LinkedHashSet<>();
		for (JiraVersion versionToMap : versionsToMap) {
			JiraVersion mappedVersion = mapVersion(jiraService, versionToMap, versionMapping, versionsToIgnore, projectKey);
			if (mappedVersion != null) {
				log.trace("version: {}  -->  {}", versionToMap, mappedVersion);
				mappedVersions.add(mappedVersion);
			}
		}

		return mappedVersions;
	}

	private JiraVersion mapVersion(JiraService jiraService, JiraVersion versionToMap, Map<String, String> versionMapping, Set<String> versionsToIgnore, String projectKey) {
		String versionName = versionToMap.getName();
		Assert.notNull(versionName, "versionName not set: " + versionToMap);

		if (versionsToIgnore.contains(versionName)) {
			log.debug("ignoring version '{}'", versionName);
			return null;
		}

		String mappedVersionName = versionMapping.get(versionName);
		if (mappedVersionName == null) {
			log.warn("no mapping defined for '{}'", versionToMap);
			return null;
		}

		List<JiraVersion> projectVersions = jiraService.getVersions(projectKey);
		JiraVersion mappedVersion = projectVersions.stream()
			.filter(version -> Objects.equals(version.getName(), mappedVersionName))
			.findFirst()
			.orElse(null);

		if (mappedVersion == null) {
			log.warn("version '{}' not found in {}", mappedVersionName, jiraService);
			return null;
		}
		return mappedVersion;
	}

}
