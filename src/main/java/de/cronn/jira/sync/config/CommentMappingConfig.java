package de.cronn.jira.sync.config;

public class CommentMappingConfig {

	private String titleBackgroundColor = "#dddddd";
	private String backgroundColor = "#eeeeee";
	private String outOfOrderTitleBackgroundColor = "#cccccc";
	private String outOfOrderBackgroundColor = "#dddddd";

	public String getTitleBackgroundColor() {
		return titleBackgroundColor;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setTitleBackgroundColor(String titleBackgroundColor) {
		this.titleBackgroundColor = titleBackgroundColor;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public String getOutOfOrderBackgroundColor() {
		return outOfOrderBackgroundColor;
	}

	public String getOutOfOrderTitleBackgroundColor() {
		return outOfOrderTitleBackgroundColor;
	}

	public void setOutOfOrderBackgroundColor(String outOfOrderBackgroundColor) {
		this.outOfOrderBackgroundColor = outOfOrderBackgroundColor;
	}

	public void setOutOfOrderTitleBackgroundColor(String outOfOrderTitleBackgroundColor) {
		this.outOfOrderTitleBackgroundColor = outOfOrderTitleBackgroundColor;
	}
}
