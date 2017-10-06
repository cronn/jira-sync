package de.cronn.jira.sync.link;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.service.JiraService;

@Service
public class JiraIssueWebLinker implements JiraIssueLinker {

	private static final Logger log = LoggerFactory.getLogger(JiraIssueWebLinker.class);

	@Override
	public JiraIssue resolveIssue(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService) {
		String key = resolveKey(fromIssue, fromJiraService, toJiraService);
		if (key == null) {
			return null;
		}
		return resolveKey(toJiraService, key);
	}

	@Override
	public String resolveKey(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService) {
		List<String> keys = resolveKeys(fromIssue, fromJiraService, toJiraService);
		if (CollectionUtils.isEmpty(keys)) {
			return null;
		} else if (keys.size() > 1) {
			throw new JiraSyncException("Illegal number of linked issues for " + fromIssue + ": " + keys);
		} else {
			return keys.get(0);
		}
	}

	private List<String> resolveKeys(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService) {
		Instant issueUpdated = fromIssue.getFields().getUpdated().toInstant();
		List<JiraRemoteLink> remoteLinks = fromJiraService.getRemoteLinks(fromIssue.getKey(), issueUpdated);
		Pattern pattern = createPattern(toJiraService);
		List<String> resolvedKeys = new ArrayList<>();
		for (JiraRemoteLink remoteLink : remoteLinks) {
			URL remoteLinkUrl = remoteLink.getObject().getUrl();
			Matcher matcher = pattern.matcher(remoteLinkUrl.toString());
			if (matcher.matches()) {
				String key = matcher.group(1);
				log.debug("{}: found remote link: {} with key {}", fromIssue, remoteLinkUrl, key);
				resolvedKeys.add(key);
			} else {
				log.debug("{}: ignoring remote link: {}", fromIssue, remoteLinkUrl);
			}
		}
		return resolvedKeys;
	}

	private Pattern createPattern(JiraService jiraService) {
		String baseUrl = jiraService.getServerInfo().getBaseUrl();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return Pattern.compile("^" + Pattern.quote(baseUrl) + "/+browse/([A-Z_]+-\\d+)$");
	}

	private JiraIssue resolveKey(JiraService toJiraService, String key) {
		Assert.hasText(key, "key must not be empty");
		JiraIssue issue = toJiraService.getIssueByKey(key);
		if (issue == null) {
			throw new JiraSyncException("Failed to resolve '" + key + "' in " + toJiraService);
		}
		return issue;
	}

	@Override
	public void linkIssue(JiraIssue fromIssue, JiraIssue toIssue, JiraService fromJiraService, JiraService toJiraService, URL iconUrl) {
		fromJiraService.addRemoteLink(fromIssue, toIssue, toJiraService, iconUrl);
	}
}
