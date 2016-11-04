package de.cronn.jira.sync.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.cronn.jira.sync.JiraSyncException;
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
			JiraField fromField = findField(fromJira, entry.getKey());
			JiraField toField = findField(toJira, entry.getValue());

			Map<String, Object> fromFields = fromIssue.getOrCreateFields().getOther();
			Object sourceValue = fromFields.get(fromField.getId());
			if (sourceValue != null) {
				if (fromField.isCustom() && !toField.isCustom()) {
					log.warn("Conversion from standard field {} to custom field {} is currently not supported", fromField.getName(), toField.getName());
				}
				if (toField.isCustom()) {
					fields.put(toField.getId(), sourceValue);
				} else {
					Map<String, Object> sourceValueMap = (Map<String, Object>) sourceValue;
					fields.put(toField.getId(), sourceValueMap.get("value"));
				}
			}
		}
		return fields;
	}

	private JiraField findField(JiraService jiraService, String fieldName) {
		List<JiraField> fields = jiraService.getFields();
		for (JiraField field : fields) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}
		throw new JiraSyncException("Field '" + fieldName + "' not found in " + jiraService);
	}

}
