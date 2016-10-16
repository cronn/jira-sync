package de.cronn.jira.sync.domain;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;

public class JiraIdResourceTest {

	private static class TestJiraIdResource extends JiraIdResource {
	}

	@Test
	public void testToString() {
		JiraIdResource idResource = new TestJiraIdResource();
		idResource.setId("10");

		assertThat(idResource.toString(), Matchers.is("JiraIdResourceTest.TestJiraIdResource[id=10]"));
	}

}