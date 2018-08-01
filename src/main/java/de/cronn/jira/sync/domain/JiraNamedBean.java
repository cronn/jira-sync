package de.cronn.jira.sync.domain;

import java.util.Collection;
import java.util.stream.Collectors;

public interface JiraNamedBean {

	String getName();

	static String getNameOrNull(JiraNamedBean namedBean) {
		return namedBean != null ? namedBean.getName() : null;
	}

	static String join(Collection<? extends JiraNamedBean> namedBeans) {
		if (namedBeans == null) {
			return null;
		}
		return namedBeans.stream()
			.map(JiraNamedBean::getName)
			.collect(Collectors.joining(", "));
	}

}
