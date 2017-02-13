package de.cronn.jira.sync.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

@Component
public class DefaultFieldMapper implements FieldMapper {

	private final static Logger log = LoggerFactory.getLogger(DefaultFieldMapper.class);

	private JiraSyncConfig jiraSyncConfig;

	@Autowired
	public void setJiraSyncConfig(JiraSyncConfig jiraSyncConfig) {
		this.jiraSyncConfig = jiraSyncConfig;
	}

	@Override
	public Map<String, Object> map(JiraIssue fromIssue, JiraService fromJira, JiraService toJira) {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (Entry<String, String> entry : jiraSyncConfig.getFieldMapping().entrySet()) {
			JiraField fromField = fromJira.findField(entry.getKey());
			JiraField toField = toJira.findField(entry.getValue());

			Map<String, Object> fromFields = fromIssue.getOrCreateFields().getOther();
			Object sourceValue = fromFields.get(fromField.getId());
			if (sourceValue != null) {
				if (!fromField.isCustom() && toField.isCustom()) {
					log.warn("Conversion from standard field {} to custom field {} is currently not supported", fromField.getName(), toField.getName());
				}
				if (toField.isCustom()) {
					fields.put(toField.getId(), sourceValue);
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> sourceValueMap = (Map<String, Object>) sourceValue;
					fields.put(toField.getId(), sourceValueMap.get("value"));
				}
			}
		}
		return fields;
	}

}
