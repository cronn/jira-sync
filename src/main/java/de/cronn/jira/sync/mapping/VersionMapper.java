package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Set;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraVersion;
import de.cronn.jira.sync.service.JiraService;

public interface VersionMapper extends NamedResourceMapper<JiraVersion> {

	Set<JiraVersion> mapSourceToTarget(JiraService jiraService, Collection<JiraVersion> versionsToMap, JiraProjectSync projectSync);

	Set<JiraVersion> mapTargetToSource(JiraService jiraService, Collection<JiraVersion> versionsToMap, JiraProjectSync projectSync);

}
