package de.cronn.jira.sync;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

final class StreamUtils {

	private StreamUtils() {
	}

	static <T, K> Predicate<T> distinctByKey(Function<? super T, K> keyExtractor) {
		Set<K> seen = ConcurrentHashMap.newKeySet();
		return value -> {
			K key = keyExtractor.apply(value);
			return seen.add(key);
		};
	}

}
