package de.cronn.jira.sync;

import static org.junit.Assert.*;

import org.junit.Test;

import de.cronn.jira.sync.config.JiraProjectSync;

public class ProjectSyncResultTest {
	@Test
	public void shouldBeFailed() throws Exception {
		//given
		ProjectSyncResult sut = new ProjectSyncResult(new JiraProjectSync(), ProjectSyncResultType.FAILED);
		//when
		//then
		assertTrue(sut.isFailed());
	}

	@Test
	public void shouldBeNotFailed() throws Exception {
		//given
		ProjectSyncResult sut = new ProjectSyncResult(new JiraProjectSync(), ProjectSyncResultType.SUCCEEDED);
		//when
		//then
		assertFalse(sut.isFailed());
	}

}