package de.cronn.jira.sync;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtils {

	private MapUtils() {
	}

	public static Map<String, String> calculateInverseMapping(Map<String, String> versionMapping) {
		Map<String, String> inverse = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : versionMapping.entrySet()) {
			Object existing = inverse.putIfAbsent(entry.getValue(), entry.getKey());
			if (existing != null) {
				throw new IllegalArgumentException("Non-unique mapping. Duplicate values for key '" + entry.getKey() + "' and '" + existing + "'");
			}
		}
		return inverse;
	}

}