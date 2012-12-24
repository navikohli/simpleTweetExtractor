package de.tangibleit.crawler.twitterUser;

public class Messages {
	public static abstract class Task {
		public abstract String getPath();
	}

	public static class CrawlUser extends Task {
		public String userName;

		@Override
		public String getPath() {
			return "statuses/user_timeline";
		}

		public CrawlUser(String userName) {
			this.userName = userName;
		}
	}

	public static class CrawlList extends Task {
		public String userName;
		public String listName;

		public CrawlList(String userName, String listName) {
			this.userName = userName;
			this.listName = listName;
		}

		@Override
		public String getPath() {
			return "lists/members";
		}
	}
}
