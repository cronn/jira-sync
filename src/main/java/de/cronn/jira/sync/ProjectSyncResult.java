package de.cronn.jira.sync;

import java.util.Map;

import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.strategy.SyncResult;

class ProjectSyncResult {
	private final JiraProjectSync projectSync;
	private final Map<SyncResult, Long> resultCounts;

	public ProjectSyncResult(JiraProjectSync projectSync, Map<SyncResult, Long> resultCounts) {
		Assert.notNull(projectSync, "projectSync required");
		Assert.notNull(projectSync, "resultCounts required");
		this.projectSync = projectSync;
		this.resultCounts = resultCounts;
	}

	public JiraProjectSync getProjectSync() {
		return projectSync;
	}

	public boolean hasFailed() {
		return getCount(SyncResult.FAILED) > 0L;
	}

	public Long getCount(SyncResult syncResult) {
		return resultCounts.getOrDefault(syncResult, 0L);
	}
}
