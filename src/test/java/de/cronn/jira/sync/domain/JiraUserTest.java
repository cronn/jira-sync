package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraUserTest {

	@Test
	public void testToString() throws Exception {
		JiraUser user = new JiraUser("user", "some.user", "Some User");
		assertThat(user).hasToString("JiraUser[key=some.user,name=user,displayName=Some User]");
	}

}