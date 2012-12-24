package de.tangibleit.crawler.twitterUser;

public class Messages {
	public static abstract class Task {
	}

	public static class CrawlUser extends Task {
		public String userName;

		public CrawlUser(String userName) {
			this.userName = userName;
		}
	}

	public static class Idle {

	}

	public static class CrawlList extends Task {
		public String userName;
		public String listName;

		public CrawlList(String userName, String listName) {
			this.userName = userName;
			this.listName = listName;
		}

	}
}
