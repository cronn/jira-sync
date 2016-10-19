package de.cronn.jira.sync;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.service.JiraService;
import de.cronn.jira.sync.strategy.ExistingTargetJiraIssueSyncStrategy;
import de.cronn.jira.sync.strategy.IssueSyncStrategy;
import de.cronn.jira.sync.strategy.MissingTargetJiraIssueSyncStrategy;
import de.cronn.jira.sync.strategy.SyncResult;

@Component
@EnableConfigurationProperties(JiraSyncConfig.class)
public class JiraSyncTask implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(JiraSyncTask.class);

	private final JiraService jiraSource;
	private final JiraService jiraTarget;
	private final JiraSyncConfig jiraSyncConfig;
	private final JiraIssueLinker jiraIssueLinker;
	private final MissingTargetJiraIssueSyncStrategy missingTargetJiraIssueSyncStrategy;
	private final ExistingTargetJiraIssueSyncStrategy existingTargetJiraIssueSyncStrategy;

	public JiraSyncTask(JiraService jiraSource, JiraService jiraTarget, JiraSyncConfig jiraSyncConfig, JiraIssueLinker jiraIssueLinker, MissingTargetJiraIssueSyncStrategy missingTargetJiraIssueSyncStrategy, ExistingTargetJiraIssueSyncStrategy existingTargetJiraIssueSyncStrategy) {
		this.jiraSource = jiraSource;
		this.jiraTarget = jiraTarget;
		this.jiraSyncConfig = jiraSyncConfig;
		this.jiraIssueLinker = jiraIssueLinker;
		this.missingTargetJiraIssueSyncStrategy = missingTargetJiraIssueSyncStrategy;
		this.existingTargetJiraIssueSyncStrategy = existingTargetJiraIssueSyncStrategy;
	}

	@Override
	public void run(String... args) throws Exception {
		if (jiraSyncConfig.isAutostart()) {
			sync();
		} else {
			log.info("not auto-starting sync");
		}
	}

	public void sync() {
		if (jiraSyncConfig.getSource() == null || jiraSyncConfig.getTarget() == null) {
			log.warn("Source or target is not defined");
			return;
		}
		try {
			jiraSource.login(jiraSyncConfig.getSource());
			jiraTarget.login(jiraSyncConfig.getTarget());

			log.info("going to link source={} with target={}", jiraSource, jiraTarget);
			log.info("jiraSource server info: {}", jiraSource.getServerInfo());
			log.info("jiraTarget server info: {}", jiraTarget.getServerInfo());

			Map<String, JiraProjectSync> projects = jiraSyncConfig.getProjects();
			if (projects.isEmpty()) {
				log.warn("No projects configured");
				return;
			}

			for (JiraProjectSync projectSync : projects.values()) {
				syncProject(jiraSource, jiraTarget, projectSync);
			}
		} catch (Exception e) {
			log.error("Synchronisation failed", e);
			throw e;
		} finally {
			jiraSource.close();
			jiraTarget.close();
		}
	}

	private void syncProject(JiraService jiraSource, JiraService jiraTarget, JiraProjectSync projectSync) {
		log.info("syncing project '{}' to '{}'", projectSync.getSourceProject(), projectSync.getTargetProject());
		String sourceFilterId = projectSync.getSourceFilterId();
		Assert.notNull(sourceFilterId, "sourceFilterId must be configured");
		List<JiraIssue> issues = jiraSource.getIssuesByFilterId(sourceFilterId);

		Map<SyncResult, Long> resultCounts = new EnumMap<>(SyncResult.class);
		for (SyncResult syncResult : SyncResult.values()) {
			resultCounts.put(syncResult, 0L);
		}

		for (JiraIssue sourceIssue : issues) {
			JiraProject project = sourceIssue.getFields().getProject();
			if (!project.getKey().equals(projectSync.getSourceProject())) {
				throw new JiraSyncException("Filter returned issue " + sourceIssue + " from unexpected project " + project);
			}
			JiraIssue targetIssue = jiraIssueLinker.resolve(sourceIssue, jiraSource, jiraTarget);
			IssueSyncStrategy syncStrategy = getSyncStrategy(targetIssue);
			SyncResult syncResult = syncStrategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
			resultCounts.compute(syncResult, (k, v) -> v + 1);
		}

		for (Entry<SyncResult, Long> entry : resultCounts.entrySet()) {
			log.info("[{} -> {}]   {} issues: {}", projectSync.getSourceProject(), projectSync.getTargetProject(), entry.getKey().getDisplayName(), entry.getValue());
		}
	}

	private IssueSyncStrategy getSyncStrategy(JiraIssue targetIssue) {
		if (targetIssue == null) {
			return missingTargetJiraIssueSyncStrategy;
		} else {
			return existingTargetJiraIssueSyncStrategy;
		}
	}


}
