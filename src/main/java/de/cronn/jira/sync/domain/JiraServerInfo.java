package de.cronn.jira.sync.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraServerInfo extends JiraResource {

	private static final long serialVersionUID = 1L;

	private String baseUrl;
	private String serverTitle;
	private String version;

	public JiraServerInfo() {
	}

	public JiraServerInfo(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getServerTitle() {
		return serverTitle;
	}

	public void setServerTitle(String serverTitle) {
		this.serverTitle = serverTitle;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("baseUrl", baseUrl)
			.append("serverTitle", serverTitle)
			.append("version", version)
			.toString();
	}
}
