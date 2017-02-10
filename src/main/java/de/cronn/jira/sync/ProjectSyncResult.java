package de.cronn.jira.sync;

import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;

class ProjectSyncResult {
	private JiraProjectSync projectSync;
	private ProjectSyncResultType resultType;

	public ProjectSyncResult(JiraProjectSync projectSync, ProjectSyncResultType resultType) {
		Assert.notNull(projectSync, "projectSync required");
		Assert.notNull(projectSync, "resultType required");
		this.projectSync = projectSync;
		this.resultType = resultType;
	}

	public JiraProjectSync getProjectSync() {
		return projectSync;
	}

	public ProjectSyncResultType getResultType() {
		return resultType;
	}

	public boolean isFailed() {
		return resultType == ProjectSyncResultType.FAILED;
	}
}
