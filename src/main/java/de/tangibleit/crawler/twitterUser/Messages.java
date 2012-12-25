package de.tangibleit.crawler.twitterUser;

public class Messages {
	public static abstract class Task {
		public int queueId; // Queue row this task belongs to

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + queueId;
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
			Task other = (Task) obj;
			if (queueId != other.queueId)
				return false;
			return true;
		}

	}

	public static class CrawlUser extends Task {

		public String userName;
		public int organizationId;

		public CrawlUser(String userName, int queueId, int organizationId) {
			this.userName = userName;
			this.organizationId = organizationId;
			this.queueId = queueId;
		}

		public CrawlUser(String userName, int queueId) {
			this(userName, queueId, -1);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + organizationId;
			result = prime * result
					+ ((userName == null) ? 0 : userName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			CrawlUser other = (CrawlUser) obj;
			if (organizationId != other.organizationId)
				return false;
			if (userName == null) {
				if (other.userName != null)
					return false;
			} else if (!userName.equals(other.userName))
				return false;
			return true;
		}

	}

	public static class Idle {

	}

	public static class CrawlList extends Task {
		public String userName;
		public String listName;
		public int organizationId;

		public CrawlList(String userName, String listName, int queueId) {
			this(userName, listName, queueId, -1);
		}

		public CrawlList(String userName, String listName, int queueId,
				int organizationId) {
			this.userName = userName;
			this.listName = listName;
			this.queueId = queueId;
			this.organizationId = organizationId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((listName == null) ? 0 : listName.hashCode());
			result = prime * result + organizationId;
			result = prime * result
					+ ((userName == null) ? 0 : userName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			CrawlList other = (CrawlList) obj;
			if (listName == null) {
				if (other.listName != null)
					return false;
			} else if (!listName.equals(other.listName))
				return false;
			if (organizationId != other.organizationId)
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
