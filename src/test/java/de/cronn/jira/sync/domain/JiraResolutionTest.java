package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class JiraResolutionTest {

	@Test
	public void testToString() {
		JiraResolution resolution = new JiraResolution("1", "resolution");
		assertThat(resolution).hasToString("JiraResolution[id=1,name=resolution]");
	}

	@Test
	public void testEqualsAndHashCodeContract() throws Exception {
		EqualsVerifier.forClass(JiraResolution.class)
			.suppress(Warning.NONFINAL_FIELDS)
			.withIgnoredFields("self")
			.verify();
	}

}
