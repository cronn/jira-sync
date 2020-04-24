package de.cronn.jira.sync;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

public class StreamUtilsTest {

	@Test
	public void testDistinctByKey() throws Exception {
		assertThat(Stream.of("abc", "xxx", "abcd", "yyy")
			.filter(StreamUtils.distinctByKey(value -> value.substring(0, 1))))
			.containsExactly("abc", "xxx", "yyy");

		assertThat(Stream.of("xx", "x", "yyy", "xxx", "yy")
			.filter(StreamUtils.distinctByKey(String::length)))
			.containsExactly("xx", "x", "yyy");

		assertThat(IntStream.range(1, 100)
			.boxed()
			.filter(StreamUtils.distinctByKey(value -> value % 10)))
			.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	}

}
