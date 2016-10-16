package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JiraResourceTest {

	private static class TestJiraResource extends JiraResource {
	}

	@Test
	public void testToString() {
		JiraResource resource = new TestJiraResource();
		resource.setSelf("self");
		assertThat(resource.toString(), is("JiraResourceTest.TestJiraResource[self=self]"));
	}

}