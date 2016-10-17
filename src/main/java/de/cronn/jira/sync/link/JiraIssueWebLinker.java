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
import org.springframework.util.CollectionUtils;

import de.cronn.jira.sync.JiraSyncException;
import de.cronn.jira.sync.config.JiraProjectSync;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraRemoteLink;
import de.cronn.jira.sync.service.JiraService;

@Service
public class JiraIssueWebLinker implements JiraIssueLinker {

	private static final Logger log = LoggerFactory.getLogger(JiraIssueWebLinker.class);

	@Override
	public JiraIssue resolve(JiraIssue sourceIssue, JiraService jiraSource, JiraService jiraTarget) {
		List<JiraIssue> jiraIssues = resolveIssues(sourceIssue, jiraSource, jiraTarget);
		if (CollectionUtils.isEmpty(jiraIssues)) {
			return null;
		} else if (jiraIssues.size() > 1) {
			throw new JiraSyncException("Illegal number of linked jira issues for " + sourceIssue + ": " + jiraIssues);
		} else {
			return jiraIssues.get(0);
		}
	}

	private List<JiraIssue> resolveIssues(JiraIssue fromIssue, JiraService fromJiraService, JiraService toJiraService) {
		Instant issueUpdated = fromIssue.getFields().getUpdated().toInstant();
		List<JiraRemoteLink> remoteLinks = fromJiraService.getRemoteLinks(fromIssue.getKey(), issueUpdated);
		String toBaseUrl = toJiraService.getServerInfo().getBaseUrl();
		Pattern pattern = Pattern.compile("^" + Pattern.quote(toBaseUrl) + "/+browse/([A-Z_]+-\\d+)$");
		List<JiraIssue> resolvedIssues = new ArrayList<>();
		for (JiraRemoteLink remoteLink : remoteLinks) {
			URL remoteLinkUrl = remoteLink.getObject().getUrl();
			Matcher matcher = pattern.matcher(remoteLinkUrl.toString());
			if (matcher.matches()) {
				String key = matcher.group(1);
				log.debug("{}: found remote link: {} with key {}", fromIssue, remoteLinkUrl, key);
				JiraIssue resolvedIssue = toJiraService.getIssueByKey(key);
				if (resolvedIssue == null) {
					throw new JiraSyncException("Failed to link " + key + " in target");
				}
				resolvedIssues.add(resolvedIssue);
			} else {
				log.debug("{}: ignoring remote link: {}", fromIssue, remoteLinkUrl);
			}
		}
		return resolvedIssues;
	}

	@Override
	public void linkIssues(JiraIssue sourceIssue, JiraIssue targetIssue, JiraService jiraSource, JiraService jiraTarget, JiraProjectSync projectSync) {
		jiraSource.addRemoteLink(sourceIssue, targetIssue, jiraTarget, projectSync.getRemoteLinkIconInSource());
		jiraTarget.addRemoteLink(targetIssue, sourceIssue, jiraSource, projectSync.getRemoteLinkIconInTarget());
	}
}
