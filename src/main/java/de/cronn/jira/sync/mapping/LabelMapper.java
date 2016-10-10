package de.cronn.jira.sync.mapping;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.cronn.jira.sync.domain.JiraIssue;

public final class LabelMapper {

	private LabelMapper() {
	}

	public static Set<String> mapLabels(JiraIssue sourceIssue) {
		Set<String> labels = sourceIssue.getFields().getLabels();
		if (labels == null) {
			return Collections.emptySet();
		} else {
			return new LinkedHashSet<>(labels);
		}
	}
}
