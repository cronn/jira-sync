package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class JiraResolutionTest {

	@Test
	public void testToString() {
		JiraResolution resolution = new JiraResolution("1", "resolution");
		assertThat(resolution.toString(), is("JiraResolution[id=1,name=resolution]"));
	}

}