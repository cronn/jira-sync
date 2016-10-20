package de.cronn.jira.sync.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import de.cronn.jira.sync.domain.JiraUser;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultUsernameReplacer implements UsernameReplacer {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("\\[~([a-zA-Z0-9_.-]+)\\]");

	@Override
	public String replaceUsernames(String inputText, JiraService jiraService) {
		if (inputText == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		Matcher matcher = USERNAME_PATTERN.matcher(inputText);
		while (matcher.find()) {
			String username = matcher.group(1);
			JiraUser jiraUser = jiraService.getUserByName(username);
			if (jiraUser != null) {
				String userLink = buildUserLink(jiraService, username);
				matcher.appendReplacement(sb, "[" + jiraUser.getDisplayName() + "|" + userLink + "]");
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String buildUserLink(JiraService jiraService, String username) {
		String baseUrl = jiraService.getServerInfo().getBaseUrl();
		return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "secure/ViewProfile.jspa?name=" + username;
	}

}
