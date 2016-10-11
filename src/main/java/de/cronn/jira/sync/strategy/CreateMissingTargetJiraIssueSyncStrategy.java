package de.cronn.jira.sync.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.mapping.DefaultSummaryMapper;
import de.cronn.jira.sync.mapping.DefaultVersionMapper;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.IssueTypeMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.service.JiraService;

@Component
public class CreateMissingTargetJiraIssueSyncStrategy implements MissingTargetJiraIssueSyncStrategy {

	private static final Logger log = LoggerFactory.getLogger(CreateMissingTargetJiraIssueSyncStrategy.class);

	private final JiraSyncConfig jiraSyncConfig;
	private final DescriptionMapper descriptionMapper;
	private final IssueTypeMapper issueTypeMapper;
	private final LabelMapper labelMapper;
	private final PriorityMapper priorityMapper;

	public CreateMissingTargetJiraIssueSyncStrategy(JiraSyncConfig jiraSyncConfig, DescriptionMapper descriptionMapper, IssueTypeMapper issueTypeMapper, LabelMapper labelMapper, PriorityMapper priorityMapper) {
		this.jiraSyncConfig = jiraSyncConfig;
		this.descriptionMapper = descriptionMapper;
		this.issueTypeMapper = issueTypeMapper;
		this.labelMapper = labelMapper;
		this.priorityMapper = priorityMapper;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraProjectSync projectSync) {
		JiraProject targetProject = jiraTarget.getProjectByKey(projectSync.getTargetProject());
		Assert.notNull(targetProject, "target project '" + projectSync.getTargetProject() + "' not found");
		log.info("going to create issue in {}", targetProject);
		JiraIssue issueToCreate = new JiraIssue(targetProject);

		copyIssueType(sourceIssue, projectSync, targetProject, issueToCreate);
		copySummary(sourceIssue, issueToCreate);
		copyDescription(sourceIssue, issueToCreate);
		copyPriority(jiraTarget, sourceIssue, issueToCreate);
		copyLabels(sourceIssue, issueToCreate);
		copyVersions(sourceIssue, issueToCreate, jiraTarget, projectSync);
		copyFixVersions(sourceIssue, issueToCreate, jiraTarget, projectSync);

		JiraIssue newIssue = jiraTarget.createIssue(issueToCreate);
		jiraSource.addRemoteLink(sourceIssue, newIssue, jiraTarget, projectSync.getRemoteLinkIconInSource());
		jiraTarget.addRemoteLink(newIssue, sourceIssue, jiraSource, projectSync.getRemoteLinkIconInTarget());

		return SyncResult.CREATED;
	}

	private void copyIssueType(JiraIssue sourceIssue, JiraProjectSync projectSync, JiraProject targetProject, JiraIssue issueToCreate) {
		JiraIssueType targetIssueType = issueTypeMapper.mapIssueType(sourceIssue, jiraSyncConfig, projectSync, targetProject);
		issueToCreate.getFields().setIssuetype(targetIssueType);
	}

	private void copyPriority(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue issueToCreate) {
		JiraPriority targetPriority = priorityMapper.mapPriority(jiraTarget, sourceIssue);
		issueToCreate.getFields().setPriority(targetPriority);
	}

	private void copyLabels(JiraIssue sourceIssue, JiraIssue issueToCreate) {
		issueToCreate.getFields().setLabels(labelMapper.mapLabels(sourceIssue));
	}

	private void copyVersions(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraTarget, JiraProjectSync projectSync) {
		issueToCreate.getFields().setVersions(DefaultVersionMapper.mapVersions(jiraTarget, sourceIssue.getFields().getVersions(), projectSync));
	}

	private void copyFixVersions(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraTarget, JiraProjectSync projectSync) {
		issueToCreate.getFields().setFixVersions(DefaultVersionMapper.mapVersions(jiraTarget, sourceIssue.getFields().getFixVersions(), projectSync));
	}

	private void copySummary(JiraIssue sourceIssue, JiraIssue issueToCreate) {
		issueToCreate.getFields().setSummary(DefaultSummaryMapper.mapSummary(sourceIssue));
	}

	private void copyDescription(JiraIssue sourceIssue, JiraIssue issueToCreate) {
		issueToCreate.getFields().setDescription(descriptionMapper.mapSourceDescription(sourceIssue));
	}

}
