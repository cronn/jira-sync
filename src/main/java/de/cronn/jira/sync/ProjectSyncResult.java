package de.cronn.jira.sync;

import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.strategy.SyncResult;

class ProjectSyncResult {
	private final JiraProjectSync projectSync;
	private final Map<SyncResult, Long> resultCounts;
	private final boolean genericException;

	public ProjectSyncResult(JiraProjectSync projectSync, Map<SyncResult, Long> resultCounts) {
		this(projectSync, resultCounts, false);
	}

	private ProjectSyncResult(JiraProjectSync projectSync, Map<SyncResult, Long> resultCounts, boolean genericException) {
		Assert.notNull(projectSync, "projectSync required");
		Assert.notNull(projectSync, "resultCounts required");
		this.projectSync = projectSync;
		this.resultCounts = resultCounts;
		this.genericException = genericException;
	}

	public static ProjectSyncResult genericException(JiraProjectSync projectSync) {
		return new ProjectSyncResult(projectSync, Collections.emptyMap(), true);
	}

	public JiraProjectSync getProjectSync() {
		return projectSync;
	}

	public boolean hasFailed() {
		return getCount(SyncResult.FAILED) > 0L || genericException;
	}

	public Long getCount(SyncResult syncResult) {
		return resultCounts.getOrDefault(syncResult, 0L);
	}
}
