package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraResolutionTest {

	@Test
	public void testToString() {
		JiraResolution resolution = new JiraResolution("1", "resolution");
		assertThat(resolution).hasToString("JiraResolution[id=1,name=resolution]");
	}

}