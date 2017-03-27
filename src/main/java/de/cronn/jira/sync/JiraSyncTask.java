package de.cronn.jira.sync;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

	public List<ProjectSyncResult> sync() {
		if (jiraSyncConfig.getSource() == null || jiraSyncConfig.getTarget() == null) {
			log.warn("Source or target is not defined");
			return Collections.emptyList();
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
				return Collections.emptyList();
			}
			return syncProjects(projects);
		} catch (Exception e) {
			log.error("Synchronisation failed", e);
			throw e;
		} finally {
			jiraSource.close();
			jiraTarget.close();
		}
	}

	private List<ProjectSyncResult> syncProjects(Map<String, JiraProjectSync> projects) {
		List<ProjectSyncResult> projectSyncResults = new ArrayList<>();
		for (JiraProjectSync projectSync : projects.values()) {
			ProjectSyncResult syncResult = syncProject(jiraSource, jiraTarget, projectSync);
			projectSyncResults.add(syncResult);
		}

		List<JiraProjectSync> failedProjects = findFailedProjects(projectSyncResults);

		if (!failedProjects.isEmpty()) {
			String formattedProjects = failedProjects.stream().map(this::format).collect(Collectors.joining(", "));
			throw new JiraSyncException("Synchronisation failed with :" + formattedProjects);
		}

		return projectSyncResults;
	}

	private List<JiraProjectSync> findFailedProjects(List<ProjectSyncResult> projectSyncResults) {
		return projectSyncResults.stream().filter(ProjectSyncResult::hasFailed)
				.map(ProjectSyncResult::getProjectSync)
				.collect(Collectors.toList());
	}

	private String format(JiraProjectSync projectSync) {
		return MessageFormat.format("[{0} -> {1}]", projectSync.getSourceProject(), projectSync.getTargetProject());
	}

	private ProjectSyncResult syncProject(JiraService jiraSource, JiraService jiraTarget, JiraProjectSync projectSync) {
		log.info("syncing project {}", format(projectSync));
		String sourceFilterId = projectSync.getSourceFilterId();
		Assert.notNull(sourceFilterId, "sourceFilterId must be configured");
		List<JiraIssue> issues = jiraSource.getIssuesByFilterId(sourceFilterId, jiraSyncConfig.getFieldMapping().keySet());

		Map<SyncResult, Long> resultCounts = new EnumMap<>(SyncResult.class);
		for (SyncResult syncResult : SyncResult.values()) {
			resultCounts.put(syncResult, 0L);
		}

		for (JiraIssue sourceIssue : issues) {
			SyncResult syncResult = syncIssue(sourceIssue, jiraSource, jiraTarget, projectSync);
			log.info("'{}' {}", sourceIssue.getKey(), syncResult.getDisplayName());
			resultCounts.compute(syncResult, (k, v) -> v + 1);
		}

		for (Entry<SyncResult, Long> entry : resultCounts.entrySet()) {
			log.info("{}   {} issues: {}", format(projectSync), entry.getKey().getDisplayName(), entry.getValue());
		}

		return new ProjectSyncResult(projectSync, resultCounts);
	}

	private SyncResult syncIssue(JiraIssue sourceIssue, JiraService jiraSource, JiraService jiraTarget, JiraProjectSync projectSync) {
		JiraProject project = sourceIssue.getFields().getProject();
		if (!project.getKey().equals(projectSync.getSourceProject())) {
			throw new JiraSyncException("Filter returned issue " + sourceIssue + " from unexpected project " + project);
		}
		JiraIssue targetIssue = jiraIssueLinker.resolveIssue(sourceIssue, jiraSource, jiraTarget);
		IssueSyncStrategy syncStrategy = getSyncStrategy(targetIssue);
		try {
			return syncStrategy.sync(jiraSource, jiraTarget, sourceIssue, targetIssue, projectSync);
		} catch (JiraSyncException e) {
			log.error("Issue synchronisation failed", e);
			return SyncResult.FAILED;
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
