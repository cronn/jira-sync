package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraIdResourceTest {

	private static class TestJiraIdResource extends JiraIdResource {
	}

	@Test
	public void testToString() {
		JiraIdResource idResource = new TestJiraIdResource();
		idResource.setId("10");

		assertThat(idResource).hasToString("JiraIdResourceTest.TestJiraIdResource[id=10]");
	}

}