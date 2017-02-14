package de.cronn.jira.sync.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraField;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.domain.JiraProject;
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
	public Map<String, Object> map(JiraIssue fromIssue, JiraService fromJira, JiraService toJira, JiraProject toProject) {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (Entry<String, String> entry : jiraSyncConfig.getFieldMapping().entrySet()) {
			JiraField fromField = fromJira.findField(entry.getKey());
			JiraField toField = toJira.findField(entry.getValue());

			Object mappedValue = mapValue(fromIssue, fromField, toField, toJira, toProject);
			if (mappedValue != null) {
				fields.put(toField.getId(), mappedValue);
			}
		}
		return fields;
	}

	@Override
	public Object mapValue(JiraIssue fromIssue, JiraField fromField, JiraField toField, JiraService toJira, JiraProject toProject) {
		Map<String, Object> fromFields = fromIssue.getOrCreateFields().getOther();
		Object sourceValue = fromFields.get(fromField.getId());

		if (sourceValue == null) {
			return null;
		}

		if (!fromField.isCustom() && toField.isCustom()) {
			log.warn("Conversion from standard field {} to custom field {} is currently not supported", fromField.getName(), toField.getName());
		}

		if (toField.isCustom()) {
			return mapCustomFieldValue(fromField, toField, sourceValue, toJira, toProject);
		} else {
			@SuppressWarnings("unchecked")
			Map<String, Object> sourceValueMap = (Map<String, Object>) sourceValue;
			return sourceValueMap.get("value");
		}
	}

	private Object mapCustomFieldValue(JiraField fromField, JiraField toField, Object sourceValue, JiraService toJira, JiraProject toProject) {
		String toFieldSchemaType = toField.getSchema().getCustom();
		String fromFieldSchemaType = fromField.getSchema().getCustom();

		if (!Objects.equals(fromFieldSchemaType, toFieldSchemaType)) {
			throw new IllegalArgumentException("Schema types of custom field " + fromField + " and " + toField + " do not match: "
				+ fromFieldSchemaType + " vs. " + toFieldSchemaType);
		}

		switch (toFieldSchemaType) {
			case "com.atlassian.jira.plugin.system.customfieldtypes:labels":
			case "com.atlassian.jira.plugin.system.customfieldtypes:textarea":
				return sourceValue;
			case "com.atlassian.jira.plugin.system.customfieldtypes:select":
				Map<String, Object> allowedValuesForCustomField = toJira.getAllowedValuesForCustomField(toProject.getKey(), toField.getId());
				@SuppressWarnings("unchecked")
				String source = (String) ((Map<String, Object>) sourceValue).get("value");
				Map<String, Map<String, String>> fieldValueMappings = jiraSyncConfig.getProjectConfigBySourceProject(toProject).getFieldValueMappings();
				Map<String, String> fieldValueMapping = fieldValueMappings.get(toField.getName());
				if (source != null && fieldValueMapping != null) {
					Assert.isTrue(fieldValueMapping.containsValue(source), "found no field value mapping for field " + toField + " with value '" + source + "' ");
					source = getKeyByValue(fieldValueMapping, source);
				}
				Object mappedValue = allowedValuesForCustomField.get(source);
				Assert.notNull(mappedValue, "Found no matching allowed value for '" + source + "' (" + toField + "). Candidates: " + allowedValuesForCustomField.keySet());
				return mappedValue;
			default:
				throw new IllegalArgumentException("Unknown schema of custom field " + toField + ": " + toFieldSchemaType);
		}
	}

	private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
		for (Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

}
