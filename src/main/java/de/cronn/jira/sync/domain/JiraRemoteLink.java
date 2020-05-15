package de.cronn.jira.sync.domain;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JiraRemoteLink {

	private static final long serialVersionUID = 1L;

	private JiraRemoteLinkObject object;

	public JiraRemoteLink() {
	}

	public JiraRemoteLink(URL url) {
		this.object = new JiraRemoteLinkObject(url);
	}

	public JiraRemoteLink(String url) {
		this(toUrl(url));
	}

	private static URL toUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Illegal url: " + url, e);
		}
	}

	public JiraRemoteLinkObject getObject() {
		return object;
	}

	public void setObject(JiraRemoteLinkObject object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("object", object)
			.toString();
	}

}
