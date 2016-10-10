package de.cronn.jira.sync.config;

import java.net.URL;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.core.io.Resource;

public class JiraConnectionProperties {

	private URL url;
	private String sshJumpHost;
	private String username;
	private String password;
	private BasicAuthentication basicAuth;
	private Resource sslTrustStore;
	private char[] sslTrustStorePassword;

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("url", url)
			.append("username", username)
			.toString();
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public void setSshJumpHost(String sshJumpHost) {
		this.sshJumpHost = sshJumpHost;
	}

	public String getSshJumpHost() {
		return sshJumpHost;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public BasicAuthentication getBasicAuth() {
		return basicAuth;
	}

	public void setBasicAuth(BasicAuthentication basicAuth) {
		this.basicAuth = basicAuth;
	}

	public Resource getSslTrustStore() {
		return sslTrustStore;
	}

	public void setSslTrustStore(Resource sslTrustStore) {
		this.sslTrustStore = sslTrustStore;
	}

	public void setSslTrustStorePassword(char[] sslTrustStorePassword) {
		this.sslTrustStorePassword = sslTrustStorePassword;
	}

	public char[] getSslTrustStorePassword() {
		return sslTrustStorePassword;
	}
}
