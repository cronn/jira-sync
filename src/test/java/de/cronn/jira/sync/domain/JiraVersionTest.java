package de.cronn.jira.sync.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class JiraVersionTest {

	@Test
	public void testToString() {
		JiraVersion version = new JiraVersion("1", "1.0");
		assertThat(version.toString(), is("JiraVersion[id=1,name=1.0]"));
	}

	@Test
	public void testEqualsHashCode() {
		EqualsVerifier.forClass(JiraVersion.class)
			.withIgnoredFields("self")
			.suppress(Warning.NONFINAL_FIELDS)
			.verify();
	}

}