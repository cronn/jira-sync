package de.cronn.jira.sync.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultTicketReferenceReplacer implements TicketReferenceReplacer {

	private static final String SEP = "\\s|[:?!…;\\.]";

	@Override
	public String replaceTicketReferences(String inputText, JiraService jiraService) {
		if (StringUtils.isBlank(inputText)) {
			return inputText;
		}
		StringBuffer sb = new StringBuffer();
		Pattern pattern = Pattern.compile("(?<=(?:" + SEP + "|^))" + "(" + buildProjectsPattern(jiraService) + "-\\d+)" + "(?=(?:" + SEP + "|$))");
		Matcher matcher = pattern.matcher(inputText);
		while (matcher.find()) {
			String issueKey = matcher.group(1);
			String issueLink = buildUserLink(jiraService, issueKey);
			matcher.appendReplacement(sb, "[" + issueKey + "|" + issueLink + "]");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String buildProjectsPattern(JiraService jiraService) {
		return "(" + jiraService.getProjects().stream()
			.map(project -> Pattern.quote(project.getKey()))
			.collect(Collectors.joining("|")) + ")";
	}

	private String buildUserLink(JiraService jiraService, String issueKey) {
		String baseUrl = jiraService.getServerInfo().getBaseUrl();
		return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "browse/" + issueKey;
	}

}
