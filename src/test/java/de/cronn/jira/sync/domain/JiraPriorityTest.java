package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraPriorityTest {

	@Test
	public void testToString() {
		JiraPriority priority = new JiraPriority("1", "prio");
		assertThat(priority).hasToString("JiraPriority[id=1,name=prio]");
	}

}