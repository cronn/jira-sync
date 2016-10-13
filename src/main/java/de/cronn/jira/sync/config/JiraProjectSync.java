package de.cronn.jira.sync.config;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraProjectSync {

	private String sourceProject;
	private String targetProject;
	private String sourceFilterId;
	private URL remoteLinkIconInSource;
	private URL remoteLinkIconInTarget;
	private String targetIssueTypeFallback;
	private Set<String> labelsToKeepInTarget;
	private List<TransitionConfig> transitions;
	private Map<String, String> versionMapping;

	public String getSourceProject() {
		return sourceProject;
	}

	public void setSourceProject(String sourceProject) {
		this.sourceProject = sourceProject;
	}

	public String getTargetProject() {
		return targetProject;
	}

	public void setTargetProject(String targetProject) {
		this.targetProject = targetProject;
	}

	public void setSourceFilterId(String sourceFilterId) {
		this.sourceFilterId = sourceFilterId;
	}

	public String getSourceFilterId() {
		return sourceFilterId;
	}

	public String getTargetIssueTypeFallback() {
		return targetIssueTypeFallback;
	}

	public void setTargetIssueTypeFallback(String targetIssueTypeFallback) {
		this.targetIssueTypeFallback = targetIssueTypeFallback;
	}

	public URL getRemoteLinkIconInTarget() {
		return remoteLinkIconInTarget;
	}

	public void setRemoteLinkIconInTarget(URL remoteLinkIconInTarget) {
		this.remoteLinkIconInTarget = remoteLinkIconInTarget;
	}

	public URL getRemoteLinkIconInSource() {
		return remoteLinkIconInSource;
	}

	public void setRemoteLinkIconInSource(URL remoteLinkIconInSource) {
		this.remoteLinkIconInSource = remoteLinkIconInSource;
	}

	public void setLabelsToKeepInTarget(Set<String> labelsToKeepInTarget) {
		this.labelsToKeepInTarget = labelsToKeepInTarget;
	}

	public Set<String> getLabelsToKeepInTarget() {
		return labelsToKeepInTarget;
	}

	public void setTransitions(List<TransitionConfig> transitions) {
		this.transitions = transitions;
	}

	public List<TransitionConfig> getTransitions() {
		return transitions;
	}

	public Map<String, String> getVersionMapping() {
		return versionMapping;
	}

	public void setVersionMapping(Map<String, String> versionMapping) {
		this.versionMapping = versionMapping;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("sourceProject", sourceProject)
			.append("targetProject", targetProject)
			.toString();
	}
}
