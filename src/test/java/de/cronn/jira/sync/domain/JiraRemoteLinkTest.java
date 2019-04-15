package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraRemoteLinkTest {

	@Test
	public void testIllegalUrl() throws Exception {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> new JiraRemoteLink("wrong-url"))
			.withMessage("Illegal url: wrong-url");
	}

}
