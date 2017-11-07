package de.cronn.jira.sync;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class MapUtilsTest {

	@Test
	public void testCalculateInverseMapping() throws Exception {
		assertThat(MapUtils.calculateInverseMapping(Collections.emptyMap())).isEmpty();
		assertThat(MapUtils.calculateInverseMapping(Collections.singletonMap("key", "value"))).containsExactly(entry("value", "key"));

		Map<String, String> sourceMap = new LinkedHashMap<>();
		sourceMap.put("key1", "value1");
		sourceMap.put("key2", "value2");
		sourceMap.put("key3", "value3");

		assertThat(MapUtils.calculateInverseMapping(sourceMap)).containsExactly(
			entry("value1", "key1"),
			entry("value2", "key2"),
			entry("value3", "key3")
		);
	}

	@Test
	public void testCalculateInverseMapping_NonUnique() throws Exception {
		Map<String, String> sourceMap = new LinkedHashMap<>();
		sourceMap.put("key1", "value1");
		sourceMap.put("key2", "value");
		sourceMap.put("key3", "value");

		try {
			MapUtils.calculateInverseMapping(sourceMap);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("Non-unique mapping. Duplicate values for key 'key3' and 'key2'");
		}
	}

}