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
import de.cronn.jira.sync.domain.JiraComponent;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultComponentMapper implements ComponentMapper {

	private static final Logger log = LoggerFactory.getLogger(DefaultComponentMapper.class);

	@Override
	public Set<JiraComponent> mapSourceToTarget(JiraService jiraService, Collection<JiraComponent> componentsToMap, JiraProjectSync projectSync) {
		Map<String, String> componentMapping = projectSync.getComponentMapping();
		if (CollectionUtils.isEmpty(componentMapping)) {
			log.warn("No component mapping configured for {}", projectSync);
			return null;
		}

		Set<String> componentsToIgnore = projectSync.getComponentsToIgnore();
		Supplier<List<JiraComponent>> targetProjectComponents = () -> jiraService.getComponents(projectSync.getTargetProject());

		return DefaultNamedResourceMapper.map(componentsToMap, componentsToIgnore, targetProjectComponents, componentMapping);
	}

	@Override
	public Set<JiraComponent> mapTargetToSource(JiraService jiraService, Collection<JiraComponent> componentsToMap, JiraProjectSync projectSync) {
		Map<String, String> componentMapping = projectSync.getComponentMapping();
		Assert.notNull(componentMapping, "No component mapping configured for " + projectSync);
		if (CollectionUtils.isEmpty(componentMapping)) {
			log.warn("No component mapping configured for {}", projectSync);
			return null;
		}
		Map<String, String> inverseComponentMapping = MapUtils.calculateInverseMapping(componentMapping);

		Supplier<List<JiraComponent>> sourceProjectComponents = () -> jiraService.getComponents(projectSync.getSourceProject());

		return DefaultNamedResourceMapper.map(
			componentsToMap, Collections.emptySet(), sourceProjectComponents, inverseComponentMapping);
	}

}
