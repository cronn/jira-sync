package de.cronn.jira.sync.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraProjectSyncTest {

	@Test
	public void testToString() throws Exception {
		JiraProjectSync projectSync = new JiraProjectSync();
		assertThat(projectSync).hasToString("JiraProjectSync[sourceProject=<null>,targetProject=<null>]");

		projectSync.setSourceProject("SOURCE");
		projectSync.setTargetProject("TARGET");
		assertThat(projectSync).hasToString("JiraProjectSync[sourceProject=SOURCE,targetProject=TARGET]");
	}

}