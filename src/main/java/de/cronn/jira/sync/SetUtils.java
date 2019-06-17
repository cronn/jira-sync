package de.cronn.jira.sync;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class SetUtils {

	private SetUtils() {
	}

	@SafeVarargs
	public static <T> Set<T> newLinkedHashSet(T... values) {
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	public static <T> Set<T> difference(Set<? extends T> universe, Set<? extends T> exclusions) {
		if (universe == null) {
			return Collections.emptySet();
		}
		if (exclusions == null) {
			return new LinkedHashSet<>(universe);
		}
		return universe.stream()
			.filter(v -> !exclusions.contains(v))
			.collect(toLinkedHashSet());
	}

	public static <T> Collector<T, ?, Set<T>> toLinkedHashSet() {
		return Collectors.toCollection(LinkedHashSet::new);
	}

}
