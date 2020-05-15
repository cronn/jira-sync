package de.cronn.jira.sync.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class JiraIssueStatusTest {

	@Test
	public void testEqualsAndHashCodeContract() throws Exception {
		EqualsVerifier.forClass(JiraIssueStatus.class)
			.suppress(Warning.NONFINAL_FIELDS)
			.withIgnoredFields("self")
			.verify();
	}

	@Test
	public void testToString() throws Exception {
		assertThat(new JiraIssueStatus()).hasToString("JiraIssueStatus[id=<null>,name=<null>]");
		assertThat(new JiraIssueStatus("1", "OPEN")).hasToString("JiraIssueStatus[id=1,name=OPEN]");
	}

}
