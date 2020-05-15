package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
	public Set<JiraVersion> mapSourceToTarget(JiraService jiraService, Collection<JiraVersion> versionsToMap,
											  JiraProjectSync projectSync) {
		Map<String, String> versionMapping = projectSync.getVersionMapping();
		if (CollectionUtils.isEmpty(versionMapping)) {
			log.warn("No version mapping configured for {}", projectSync);
			return null;
		}

		Set<String> versionsToIgnore = projectSync.getVersionsToIgnore();
		Supplier<List<JiraVersion>> targetProjectVersions = () -> jiraService.getVersions(projectSync.getTargetProject());

		return DefaultNamedResourceMapper.map(versionsToMap, versionsToIgnore, targetProjectVersions, versionMapping);
	}

	@Override
	public Set<JiraVersion> mapTargetToSource(JiraService jiraService, Collection<JiraVersion> versionsToMap,
											  JiraProjectSync projectSync) {
		Map<String, String> versionMapping = projectSync.getVersionMapping();
		Assert.notNull(versionMapping, "No version mapping configured for " + projectSync);
		if (CollectionUtils.isEmpty(versionMapping)) {
			log.warn("No version mapping configured for {}", projectSync);
			return null;
		}
		Map<String, String> inverseVersionMapping = MapUtils.calculateInverseMapping(versionMapping);

		Supplier<List<JiraVersion>> sourceProjectVersions = () -> jiraService.getVersions(projectSync.getSourceProject());

		return DefaultNamedResourceMapper.map(
			versionsToMap, Collections.emptySet(), sourceProjectVersions, inverseVersionMapping);
	}

}
