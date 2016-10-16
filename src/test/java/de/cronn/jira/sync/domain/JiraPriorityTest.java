package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class JiraPriorityTest {

	@Test
	public void testToString() {
		JiraPriority priority = new JiraPriority("1", "prio");
		assertThat(priority.toString(), is("JiraPriority[id=1,name=prio]"));
	}

}