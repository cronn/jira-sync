package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JiraTransitionTest {

	@Test
	public void testToString() {
		JiraTransition transition = new JiraTransition("1", "transition", null);
		assertThat(transition.toString(), is("JiraTransition[id=1,name=transition,to=<null>]"));
	}

}