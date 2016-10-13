package de.cronn.jira.sync.domain;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;

public class JiraRemoteLinkTest {

	@Test
	public void testIllegalUrl() throws Exception {
		try {
			new JiraRemoteLink("wrong-url");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), Matchers.is("Illegal url: wrong-url"));
		}
	}


}