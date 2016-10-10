package de.cronn.jira.sync.domain;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public abstract class JiraResource {

	private URL self;

	protected JiraResource() {
	}

	public void setSelf(URL self) {
		this.self = self;
	}

	public URL getSelf() {
		return self;
	}
}
