package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraRemoteLinkTest {

	@Test
	public void testIllegalUrl() throws Exception {
		try {
			new JiraRemoteLink("wrong-url");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessage("Illegal url: wrong-url");
		}
	}


}