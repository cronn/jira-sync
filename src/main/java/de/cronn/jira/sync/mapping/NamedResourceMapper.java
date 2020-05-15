package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Set;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraNamedResource;
import de.cronn.jira.sync.service.JiraService;

public interface NamedResourceMapper<T extends JiraNamedResource> {

	Set<T> mapSourceToTarget(JiraService jiraService, Collection<T> versionsToMap, JiraProjectSync projectSync);

	Set<T> mapTargetToSource(JiraService jiraService, Collection<T> versionsToMap, JiraProjectSync projectSync);

}
