package de.cronn.jira.sync.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraIssueFields;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultDescriptionMapper implements DescriptionMapper {

	private static final Pattern PANEL_PATTERN = Pattern.compile("\\{panel[^\\\\}]*\\}.*?\\{panel\\}\\s*", Pattern.DOTALL);
	private static final Pattern PANEL_START_PATTERN = Pattern.compile("\\{(panel[^\\\\}]*)\\}");

	private UsernameReplacer usernameReplacer;

	@Autowired
	public void setUsernameReplacer(UsernameReplacer usernameReplacer) {
		this.usernameReplacer = usernameReplacer;
	}

	@Override
	public String mapSourceDescription(JiraIssue sourceIssue, JiraService jiraSource) {
		String sourceDescription = getDescription(sourceIssue);
		return mapSourceDescription(sourceDescription, jiraSource);
	}

	@Override
	public String getDescription(JiraIssue sourceIssue) {
		JiraIssueFields fields = sourceIssue.getFields();
		Assert.notNull(fields, "fields must not be null");
		return normalizeDescription(fields.getDescription());
	}

	@Override
	public String mapTargetDescription(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource) {
		String sourceDescription = getDescription(sourceIssue);
		String targetDescription = getDescription(targetIssue);
		return mapTargetDescription(sourceDescription, targetDescription, jiraSource);
	}

	private String normalizeDescription(String description) {
		if (description == null) {
			return null;
		}
		return description.replaceAll("\\r\\n", "\n").trim();
	}

	public String mapSourceDescription(String sourceDescription, JiraService jiraSource) {
		if (StringUtils.isEmpty(sourceDescription)) {
			return "";
		} else {
			String descriptionWithReplacedUsernames = usernameReplacer.replaceUsernames(sourceDescription, jiraSource);
			String normalizedSourceDescription = normalizeDescription(descriptionWithReplacedUsernames);
			String escapedSourceDescription = PANEL_START_PATTERN.matcher(normalizedSourceDescription).replaceAll("\\\\{$1\\\\}");
			return "{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\n" + escapedSourceDescription + "\n{panel}\n\n";
		}
	}

	public String mapTargetDescription(String sourceDescription, String targetDescription, JiraService jiraSource) {
		if (StringUtils.isEmpty(sourceDescription) && StringUtils.isEmpty(targetDescription)) {
			return null;
		}

		if (targetDescription == null) {
			targetDescription = "";
		}

		if (sourceDescription == null) {
			sourceDescription = "";
		}

		targetDescription = targetDescription.replaceFirst(Pattern.quote(sourceDescription), "");
		Matcher panelMatcher = PANEL_PATTERN.matcher(targetDescription);
		String mappedSourceDescription = mapSourceDescription(sourceDescription, jiraSource);
		if (panelMatcher.find()) {
			targetDescription = panelMatcher.replaceFirst(Matcher.quoteReplacement(mappedSourceDescription));
		} else {
			targetDescription = mappedSourceDescription + targetDescription;
		}

		return targetDescription.trim();
	}
}
