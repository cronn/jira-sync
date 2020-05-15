package de.cronn.jira.sync.mapping;

import static de.cronn.jira.sync.mapping.DefaultNamedResourceMapper.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import de.cronn.jira.sync.domain.JiraNamedResource;

@RunWith(MockitoJUnitRunner.class)
public class DefaultNamedResourceMapperTest {

	private static class TestResource extends JiraNamedResource {
		private static final long serialVersionUID = 1L;

		public TestResource(String id, String name) {
			super(id, name);
		}
	}

	private static final TestResource SOURCE_RESOURCE_1 = new TestResource("s1", "source-1");
	private static final TestResource SOURCE_RESOURCE_2 = new TestResource("s2", "source-2");
	private static final TestResource TARGET_RESOURCE_1 = new TestResource("t1", "target-1");
	private static final TestResource TARGET_RESOURCE_2 = new TestResource("t2", "target-2");

	private static final Supplier<List<TestResource>> TARGET_RESOURCES =
		() -> Arrays.asList(TARGET_RESOURCE_1, TARGET_RESOURCE_2);

	private static final Map<String, String> MAPPING = new LinkedHashMap<>();
	static {
		MAPPING.put(SOURCE_RESOURCE_1.getName(), TARGET_RESOURCE_1.getName());
		MAPPING.put(SOURCE_RESOURCE_2.getName(), TARGET_RESOURCE_2.getName());
	}

	@Test
	public void testMap_NullResourcesToMap() throws Exception {
		Set<TestResource> mappedResources = map(null, Collections.singleton("name"), TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_EmptyResourcesToMap() throws Exception {
		Set<TestResource> mappedResources = map(
			Collections.emptySet(), Collections.singleton("name"), TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_NullIgnoredResources() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_2);

		Set<TestResource> mappedResources = map(resources, null, TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).containsExactly(TARGET_RESOURCE_2);
	}

	@Test
	public void testMap_EmptyIgnoredResources() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_1);

		Set<TestResource> mappedResources = map(resources, Collections.emptySet(), TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).containsExactly(TARGET_RESOURCE_1);
	}

	@Test
	public void testMap_NullTargetResources() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_2);

		Set<TestResource> mappedResources = map(resources, Collections.singleton("name"), null, MAPPING);

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_EmptyTargetResources() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_1);

		Set<TestResource> mappedResources = map(resources, Collections.singleton("name"), Collections::emptyList, MAPPING);

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_NullMapping() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_2);

		Set<TestResource> mappedResources = map(
			resources, Collections.singleton("name"), TARGET_RESOURCES, null);

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_EmptyMapping() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_2);

		Set<TestResource> mappedResources = map(
			resources, Collections.singleton("name"), TARGET_RESOURCES, Collections.emptyMap());

		assertThat(mappedResources).isEmpty();
	}

	@Test
	public void testMap_SingleResource() throws Exception {
		List<TestResource> resources = Collections.singletonList(SOURCE_RESOURCE_2);

		Set<TestResource> mappedResources = map(resources, null, TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).containsExactly(TARGET_RESOURCE_2);
	}

	@Test
	public void testMap_MultipleResources() throws Exception {
		List<TestResource> resources = Arrays.asList(SOURCE_RESOURCE_2, SOURCE_RESOURCE_1);

		Set<TestResource> mappedResources = map(resources, null, TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).containsExactly(TARGET_RESOURCE_2, TARGET_RESOURCE_1);
	}

	@Test
	public void testMap_IgnoredResources() throws Exception {
		List<TestResource> resources = Arrays.asList(SOURCE_RESOURCE_2, SOURCE_RESOURCE_1);

		Set<TestResource> mappedResources = map(
			resources, Collections.singleton(SOURCE_RESOURCE_2.getName()), TARGET_RESOURCES, MAPPING);

		assertThat(mappedResources).containsExactly(TARGET_RESOURCE_1);
	}

}
