package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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

import de.cronn.jira.sync.JiraSyncException;
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
		return mapVersions(jiraService, versionsToMap, versionMapping, project);
	}

	@Override
	public Set<JiraVersion> mapTargetToSource(JiraService jiraService, Collection<JiraVersion> versionsToMap, JiraProjectSync projectSync) {
		Map<String, String> inverseVersionMapping = calculateInverseMapping(projectSync.getVersionMapping());
		String project = projectSync.getSourceProject();
		return mapVersions(jiraService, versionsToMap, inverseVersionMapping, project);
	}

	private static Map<String, String> calculateInverseMapping(Map<String, String> versionMapping) {
		Map<String, String> inverse = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : versionMapping.entrySet()) {
			Object old = inverse.put(entry.getValue(), entry.getKey());
			Assert.isNull(old, "Non-unique version mapping");
		}
		return inverse;
	}

	private Set<JiraVersion> mapVersions(JiraService jiraService, Collection<JiraVersion> versionsToMap, Map<String, String> versionMapping, String project) {
		if (versionMapping == null) {
			log.warn("no version mapping configured for project '{}'", project);
			return Collections.emptySet();
		}

		if (CollectionUtils.isEmpty(versionsToMap)) {
			return Collections.emptySet();
		}

		List<JiraVersion> projectVersions = jiraService.getVersions(project);

		Set<JiraVersion> mappedVersions = new LinkedHashSet<>();
		for (JiraVersion versionToMap : versionsToMap) {
			String versionName = versionToMap.getName();
			if (versionName == null) {
				throw new JiraSyncException("versionName not set: " + versionToMap);
			}

			String mappedVersionName = versionMapping.get(versionName);
			if (mappedVersionName == null) {
				log.warn("no mapping defined for {}", versionToMap);
				continue;
			}

			JiraVersion mappedVersion = projectVersions.stream()
				.filter(version -> Objects.equals(version.getName(), mappedVersionName))
				.findFirst()
				.orElse(null);

			if (mappedVersion == null) {
				log.warn("version '{}' not found in {}", mappedVersionName, jiraService);
				continue;
			}

			Assert.notNull(mappedVersion);
			log.trace("version: {}  -->  {}", versionToMap, mappedVersion);
			mappedVersions.add(mappedVersion);
		}

		return mappedVersions;
	}
}
