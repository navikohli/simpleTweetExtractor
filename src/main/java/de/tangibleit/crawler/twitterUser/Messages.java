package de.tangibleit.crawler.twitterUser;

public class Messages {
	public static abstract class Task {
	}

	public static class CrawlUser extends Task {
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CrawlUser other = (CrawlUser) obj;
			if (userName == null) {
				if (other.userName != null)
					return false;
			} else if (!userName.equals(other.userName))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((userName == null) ? 0 : userName.hashCode());
			return result;
		}

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

		@Override
		public int hashCode() {
			final int prime = 37;
			int result = 1;
			result = prime * result
					+ ((listName == null) ? 0 : listName.hashCode());
			result = prime * result
					+ ((userName == null) ? 0 : userName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CrawlList other = (CrawlList) obj;
			if (listName == null) {
				if (other.listName != null)
					return false;
			} else if (!listName.equals(other.listName))
				return false;
			if (userName == null) {
				if (other.userName != null)
					return false;
			} else if (!userName.equals(other.userName))
				return false;
			return true;
		}

	}
}
