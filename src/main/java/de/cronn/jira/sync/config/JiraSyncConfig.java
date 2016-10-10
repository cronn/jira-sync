package de.cronn.jira.sync.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="de.cronn.jira")
public class JiraSyncConfig {

	private boolean autostart = true;
	private JiraConnectionProperties source;
	private JiraConnectionProperties target;
	private List<JiraProjectSync> projects;

	private Map<String, String> priorityMapping;
	private Map<String, String> issueTypeMapping;
	private Map<String, String> resolutionMapping;

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

	public void setProjects(List<JiraProjectSync> projects) {
		this.projects = projects;
	}

	public List<JiraProjectSync> getProjects() {
		return projects;
	}

	public void setPriorityMapping(Map<String, String> priorityMapping) {
		this.priorityMapping = priorityMapping;
	}

	public Map<String, String> getPriorityMapping() {
		return priorityMapping;
	}

	public Map<String, String> getIssueTypeMapping() {
		return issueTypeMapping;
	}

	public void setIssueTypeMapping(Map<String, String> issueTypeMapping) {
		this.issueTypeMapping = issueTypeMapping;
	}

	public void setResolutionMapping(Map<String, String> resolutionMapping) {
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