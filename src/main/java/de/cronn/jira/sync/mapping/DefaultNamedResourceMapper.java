package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.domain.JiraNamedResource;

public final class DefaultNamedResourceMapper {

	private static final Logger log = LoggerFactory.getLogger(DefaultNamedResourceMapper.class);

	private DefaultNamedResourceMapper() {
	}

	static <T extends JiraNamedResource> Set<T> map(Collection<T> resourcesToMap, Set<String> resourceNamesToIgnore,
													Supplier<List<T>> targetResourceSupplier,
													Map<String, String> nameMapping) {
		if (CollectionUtils.isEmpty(resourcesToMap)
			|| targetResourceSupplier == null
			|| CollectionUtils.isEmpty(nameMapping)) {
			return Collections.emptySet();
		}

		if (resourceNamesToIgnore == null) {
			resourceNamesToIgnore = Collections.emptySet();
		}


		List<T> targetResources = targetResourceSupplier.get();

		Set<T> mappedResources = new LinkedHashSet<>();
		for (T resourceToMap : resourcesToMap) {
			T mappedResource = map(resourceToMap, resourceNamesToIgnore, targetResources, nameMapping);
			if (mappedResource != null) {
				log.trace("Mapping resource: {}  -->  {}", resourceToMap, mappedResource);
				mappedResources.add(mappedResource);
			}
		}

		return mappedResources;
	}

	private static <T extends JiraNamedResource> T map(T resourceToMap, Set<String> resourceNamesToIgnore,
													   List<T> targetResources,
													   Map<String, String> nameMapping) {
		String resourceName = resourceToMap.getName();
		Assert.notNull(resourceName, "Resource name not set: " + resourceToMap);

		if (resourceNamesToIgnore.contains(resourceName)) {
			log.debug("Not mapping ignored resource '{}'", resourceName);
			return null;
		}

		String mappedResourceName = nameMapping.get(resourceName);
		if (mappedResourceName == null) {
			log.warn("No mapping defined for '{}'", resourceToMap);
			return null;
		}

		T mappedResource = targetResources.stream()
			.filter(resource -> Objects.equals(resource.getName(), mappedResourceName))
			.findFirst()
			.orElse(null);

		if (mappedResource == null) {
			log.warn("Resource '{}' not found in {}", mappedResourceName, targetResources);
			return null;
		}

		return mappedResource;
	}

}
