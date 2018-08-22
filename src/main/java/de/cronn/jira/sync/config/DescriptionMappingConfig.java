package de.cronn.jira.sync.config;

public class DescriptionMappingConfig {

	private String panelTitleBackgroundColor = "#dddddd";
	private String panelBackgroundColor = "#eeeeee";

	public String getPanelBackgroundColor() {
		return panelBackgroundColor;
	}

	public String getPanelTitleBackgroundColor() {
		return panelTitleBackgroundColor;
	}

	public void setPanelBackgroundColor(String panelBackgroundColor) {
		this.panelBackgroundColor = panelBackgroundColor;
	}

	public void setPanelTitleBackgroundColor(String panelTitleBackgroundColor) {
		this.panelTitleBackgroundColor = panelTitleBackgroundColor;
	}
}
