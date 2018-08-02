package de.cronn.jira.sync;

import static de.cronn.jira.sync.SetUtils.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class SetUtilsTest {

	@Test
	public void testNewLinkedHashSet() throws Exception {
		assertThat(newLinkedHashSet("a", "b", "c")).containsExactly("a", "b", "c");
		assertThat(newLinkedHashSet()).isEmpty();
		assertThat(newLinkedHashSet(3, 2, 1)).containsExactly(3, 2, 1);
	}

	@Test
	public void testDifference() throws Exception {
		assertThat(difference(null, null)).isEmpty();
		assertThat(difference(null, newLinkedHashSet("a", "b", "c"))).isEmpty();
		assertThat(difference(newLinkedHashSet("a", "b", "c"), null)).containsExactly("a", "b", "c");
		assertThat(difference(newLinkedHashSet("a", "b", "c"), newLinkedHashSet("a", "b", "c"))).isEmpty();
		assertThat(difference(newLinkedHashSet("a", "b", "c"), newLinkedHashSet("b"))).containsExactly("a", "c");
		assertThat(difference(newLinkedHashSet("b"), newLinkedHashSet("a", "b", "c"))).isEmpty();
	}

}