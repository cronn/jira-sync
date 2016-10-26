package de.cronn.jira.sync.config;

public class CacheConfig {

	private static final String DEFAULT_DIRECTORY = "cache";

	private boolean persistent;
	private String directory = DEFAULT_DIRECTORY;

	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
}
