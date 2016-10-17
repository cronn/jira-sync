package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class JiraTransitionTest {

	@Test
	public void testToString() {
		JiraTransition transition = new JiraTransition("1", "transition", null);
		assertThat(transition).hasToString("JiraTransition[id=1,name=transition,to=<null>]");
	}

}