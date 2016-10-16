package de.cronn.jira.sync.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="de.cronn.jira.sync")
public class JiraSyncConfig {

	private boolean autostart = true;
	private JiraConnectionProperties source;
	private JiraConnectionProperties target;
	private LinkedHashMap<String, JiraProjectSync> projects = new LinkedHashMap<>();

	private LinkedHashMap<String, String> priorityMapping = new LinkedHashMap<>();
	private LinkedHashMap<String, String> issueTypeMapping = new LinkedHashMap<>();
	private LinkedHashMap<String, String> resolutionMapping = new LinkedHashMap<>();

	public JiraConnectionProperties getSource() {
		return source;
	}

	public void setSource(JiraConnectionProperties source) {
		this.source = source;
	}

	public JiraConnectionProperties getTarget() {
		return target;
	}

	public void setTarget(JiraConnectionProperties target) {
		this.target = target;
	}

	public void setProjects(LinkedHashMap<String, JiraProjectSync> projects) {
		this.projects = projects;
	}

	public Map<String, JiraProjectSync> getProjects() {
		return projects;
	}

	public void setPriorityMapping(LinkedHashMap<String, String> priorityMapping) {
		this.priorityMapping = priorityMapping;
	}

	public Map<String, String> getPriorityMapping() {
		return priorityMapping;
	}

	public Map<String, String> getIssueTypeMapping() {
		return issueTypeMapping;
	}

	public void setIssueTypeMapping(LinkedHashMap<String, String> issueTypeMapping) {
		this.issueTypeMapping = issueTypeMapping;
	}

	public void setResolutionMapping(LinkedHashMap<String, String> resolutionMapping) {
		this.resolutionMapping = resolutionMapping;
	}

	public Map<String, String> getResolutionMapping() {
		return resolutionMapping;
	}

	public void setAutostart(boolean autostart) {
		this.autostart = autostart;
	}

	public boolean isAutostart() {
		return autostart;
	}
}