package de.cronn.jira.sync.strategy;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraComment;
import de.cronn.jira.sync.domain.JiraComments;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueType;
import de.cronn.jira.sync.domain.JiraPriority;
import de.cronn.jira.sync.domain.JiraProject;
import de.cronn.jira.sync.link.JiraIssueLinker;
import de.cronn.jira.sync.mapping.CommentMapper;
import de.cronn.jira.sync.mapping.ComponentMapper;
import de.cronn.jira.sync.mapping.FieldMapper;
import de.cronn.jira.sync.mapping.DescriptionMapper;
import de.cronn.jira.sync.mapping.IssueTypeMapper;
import de.cronn.jira.sync.mapping.LabelMapper;
import de.cronn.jira.sync.mapping.PriorityMapper;
import de.cronn.jira.sync.mapping.SummaryMapper;
import de.cronn.jira.sync.mapping.VersionMapper;
import de.cronn.jira.sync.service.JiraService;

@Component
public class CreateMissingTargetJiraIssueSyncStrategy implements MissingTargetJiraIssueSyncStrategy {

	private static final Logger log = LoggerFactory.getLogger(CreateMissingTargetJiraIssueSyncStrategy.class);

	private JiraSyncConfig jiraSyncConfig;
	private SummaryMapper summaryMapper;
	private DescriptionMapper descriptionMapper;
	private IssueTypeMapper issueTypeMapper;
	private LabelMapper labelMapper;
	private PriorityMapper priorityMapper;
	private VersionMapper versionMapper;
	private ComponentMapper componentMapper;
	private CommentMapper commentMapper;
	private JiraIssueLinker issueLinker;
	private FieldMapper fieldMapper;

	@Autowired
	public void setJiraSyncConfig(JiraSyncConfig jiraSyncConfig) {
		this.jiraSyncConfig = jiraSyncConfig;
	}

	@Autowired
	public void setSummaryMapper(SummaryMapper summaryMapper) {
		this.summaryMapper = summaryMapper;
	}

	@Autowired
	public void setDescriptionMapper(DescriptionMapper descriptionMapper) {
		this.descriptionMapper = descriptionMapper;
	}

	@Autowired
	public void setIssueTypeMapper(IssueTypeMapper issueTypeMapper) {
		this.issueTypeMapper = issueTypeMapper;
	}

	@Autowired
	public void setLabelMapper(LabelMapper labelMapper) {
		this.labelMapper = labelMapper;
	}

	@Autowired
	public void setVersionMapper(VersionMapper versionMapper) {
		this.versionMapper = versionMapper;
	}

	@Autowired
	public void setComponentMapper(ComponentMapper componentMapper) {
		this.componentMapper = componentMapper;
	}

	@Autowired
	public void setPriorityMapper(PriorityMapper priorityMapper) {
		this.priorityMapper = priorityMapper;
	}

	@Autowired
	public void setCommentMapper(CommentMapper commentMapper) {
		this.commentMapper = commentMapper;
	}

	@Autowired
	public void setIssueLinker(JiraIssueLinker issueLinker) {
		this.issueLinker = issueLinker;
	}

	@Autowired
	public void setFieldMapper(FieldMapper fieldMapper) {
		this.fieldMapper = fieldMapper;
	}

	@Override
	public SyncResult sync(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraProjectSync projectSync) {
		JiraProject targetProject = jiraTarget.getProjectByKey(projectSync.getTargetProject());
		Assert.notNull(targetProject, "target project '" + projectSync.getTargetProject() + "' not found");
		log.info("creating issue for {} in {}", sourceIssue.getKey(), targetProject);
		JiraIssue issueToCreate = new JiraIssue(targetProject);

		copyIssueType(sourceIssue, projectSync, targetProject, issueToCreate);
		copySummary(sourceIssue, issueToCreate);
		copyDescription(sourceIssue, issueToCreate, jiraSource);
		copyPriority(jiraTarget, sourceIssue, issueToCreate);
		copyLabels(sourceIssue, issueToCreate);
		copyVersions(sourceIssue, issueToCreate, jiraTarget, projectSync);
		copyFixVersions(sourceIssue, issueToCreate, jiraTarget, projectSync);
		copyComponents(sourceIssue, issueToCreate, jiraTarget, projectSync);
		copyCustomFields(jiraSource, jiraTarget, sourceIssue, issueToCreate);

		JiraIssue newIssue = jiraTarget.createIssue(issueToCreate);
		linkIssues(jiraSource, jiraTarget, sourceIssue, projectSync, newIssue);

		if (projectSync.isCopyCommentsToTarget()) {
			copyComments(sourceIssue, jiraSource, newIssue, jiraTarget);
		}

		return SyncResult.CREATED;
	}

	private void copyCustomFields(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue issueToCreate) {
		JiraProject targetProject = issueToCreate.getFields().getProject();
		Map<String, Object> mappedFields = fieldMapper.map(sourceIssue, jiraSource, jiraTarget, targetProject);
		for (Entry<String, Object> entry : mappedFields.entrySet()) {
			issueToCreate.getFields().setOther(entry.getKey(), entry.getValue());
		}
	}

	private void linkIssues(JiraService jiraSource, JiraService jiraTarget, JiraIssue sourceIssue, JiraProjectSync projectSync, JiraIssue newIssue) {
		issueLinker.linkIssue(sourceIssue, newIssue, jiraSource, jiraTarget, projectSync.getRemoteLinkIconInSource());
		issueLinker.linkIssue(newIssue, sourceIssue, jiraTarget, jiraSource, projectSync.getRemoteLinkIconInTarget());
	}

	private void copyIssueType(JiraIssue sourceIssue, JiraProjectSync projectSync, JiraProject targetProject, JiraIssue issueToCreate) {
		JiraIssueType targetIssueType = issueTypeMapper.mapIssueType(sourceIssue, jiraSyncConfig, projectSync, targetProject);
		issueToCreate.getOrCreateFields().setIssuetype(targetIssueType);
	}

	private void copyPriority(JiraService jiraTarget, JiraIssue sourceIssue, JiraIssue issueToCreate) {
		JiraPriority targetPriority = priorityMapper.mapPriority(jiraTarget, sourceIssue);
		issueToCreate.getOrCreateFields().setPriority(targetPriority);
	}

	private void copyLabels(JiraIssue sourceIssue, JiraIssue issueToCreate) {
		issueToCreate.getOrCreateFields().setLabels(labelMapper.mapLabels(sourceIssue));
	}

	private void copyVersions(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraTarget, JiraProjectSync projectSync) {
		issueToCreate.getOrCreateFields().setVersions(versionMapper.mapSourceToTarget(jiraTarget, sourceIssue.getFields().getVersions(), projectSync));
	}

	private void copyFixVersions(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraTarget, JiraProjectSync projectSync) {
		issueToCreate.getOrCreateFields().setFixVersions(versionMapper.mapSourceToTarget(jiraTarget, sourceIssue.getFields().getFixVersions(), projectSync));
	}

	private void copyComponents(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraTarget, JiraProjectSync projectSync) {
		issueToCreate.getOrCreateFields().setComponents(componentMapper.mapSourceToTarget(jiraTarget, sourceIssue.getFields().getComponents(), projectSync));
	}

	private void copySummary(JiraIssue sourceIssue, JiraIssue issueToCreate) {
		issueToCreate.getOrCreateFields().setSummary(summaryMapper.mapSummary(sourceIssue));
	}

	private void copyDescription(JiraIssue sourceIssue, JiraIssue issueToCreate, JiraService jiraSource) {
		issueToCreate.getOrCreateFields().setDescription(descriptionMapper.mapSourceDescription(sourceIssue, jiraSource));
	}

	private void copyComments(JiraIssue sourceIssue, JiraService jiraSource, JiraIssue newIssue, JiraService jiraTarget) {
		JiraComments comment = sourceIssue.getFields().getComment();
		if (comment != null && comment.getComments() != null) {
			for (JiraComment jiraComment : comment.getComments()) {
				String commentText = commentMapper.map(sourceIssue, jiraComment, jiraSource, false);
				jiraTarget.addComment(newIssue.getKey(), commentText);
			}
		}
	}

}
