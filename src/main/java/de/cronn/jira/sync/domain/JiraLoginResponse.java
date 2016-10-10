package de.cronn.jira.sync.domain;

public class JiraLoginResponse {

	private JiraSession session;
	private LoginInfo loginInfo;

	public JiraSession getSession() {
		return session;
	}

	public void setSession(JiraSession session) {
		this.session = session;
	}

	public LoginInfo getLoginInfo() {
		return loginInfo;
	}

	public void setLoginInfo(LoginInfo loginInfo) {
		this.loginInfo = loginInfo;
	}

	public static class LoginInfo {

		private int failedLoginCount;
		private int loginCount;
		private String lastFailedLoginTime;
		private String previousLoginTime;

		public int getFailedLoginCount() {
			return failedLoginCount;
		}

		public void setFailedLoginCount(int failedLoginCount) {
			this.failedLoginCount = failedLoginCount;
		}

		public int getLoginCount() {
			return loginCount;
		}

		public void setLoginCount(int loginCount) {
			this.loginCount = loginCount;
		}

		public String getLastFailedLoginTime() {
			return lastFailedLoginTime;
		}

		public void setLastFailedLoginTime(String lastFailedLoginTime) {
			this.lastFailedLoginTime = lastFailedLoginTime;
		}

		public String getPreviousLoginTime() {
			return previousLoginTime;
		}

		public void setPreviousLoginTime(String previousLoginTime) {
			this.previousLoginTime = previousLoginTime;
		}
	}

}