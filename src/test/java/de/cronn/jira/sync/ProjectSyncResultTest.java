package de.cronn.jira.sync;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.strategy.SyncResult;

public class ProjectSyncResultTest {

	@Test
	public void shouldBeFailed() throws Exception {
		//given
		Map<SyncResult, Long> resultCounts = Collections.singletonMap(SyncResult.FAILED, 10L);
		ProjectSyncResult sut = new ProjectSyncResult(new JiraProjectSync(), resultCounts);
		//when
		//then
		assertTrue(sut.hasFailed());
	}

	@Test
	public void shouldBeNotFailed() throws Exception {
		//given
		Map<SyncResult, Long> resultCounts = new LinkedHashMap<>();
		ProjectSyncResult sut = new ProjectSyncResult(new JiraProjectSync(), resultCounts);
		//when
		//then
		assertFalse(sut.hasFailed());
	}

}