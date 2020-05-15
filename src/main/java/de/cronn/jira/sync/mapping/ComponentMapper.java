package de.cronn.jira.sync.mapping;

import java.util.Collection;
import java.util.Set;

import javax.swing.*;

import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraComponent;
import de.cronn.jira.sync.service.JiraService;

public interface ComponentMapper extends NamedResourceMapper<JiraComponent> {

	Set<JiraComponent> mapSourceToTarget(JiraService jiraService, Collection<JiraComponent> componentsToMap, JiraProjectSync projectSync);

	Set<JiraComponent> mapTargetToSource(JiraService jiraService, Collection<JiraComponent> componentsToMap, JiraProjectSync projectSync);

}
