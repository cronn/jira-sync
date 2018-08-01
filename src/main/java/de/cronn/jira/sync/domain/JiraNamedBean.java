package de.cronn.jira.sync.domain;

public interface JiraNamedBean {

	String getName();

	static String getNameOrNull(JiraNamedBean namedBean) {
		return namedBean != null ? namedBean.getName() : null;
	}

}
