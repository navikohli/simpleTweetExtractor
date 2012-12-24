package de.tangibleit.crawler.twitterUser;

import twitter4j.PagableResponseList;
import twitter4j.TwitterException;
import twitter4j.User;
import de.tangibleit.crawler.twitterUser.Messages.CrawlList;
import de.tangibleit.crawler.twitterUser.Messages.CrawlUser;
import de.tangibleit.crawler.twitterUser.Messages.Task;
import akka.actor.UntypedActor;

public class ListWorker extends Worker<CrawlList> {

	@Override
	protected void execute(CrawlList msg) {
		try {
			PagableResponseList<User> members = twitter.getUserListMembers(
					msg.userName, msg.listName, -1);
			do {
				for (User u : members) {
					getContext().parent()
							.tell(new CrawlUser(u.getScreenName()));
				}
				members = twitter.getUserListMembers(msg.userName,
						msg.listName, members.getNextCursor());
			} while (members.hasNext());
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected String getPath() {
		return "/lists/members";
	}

}
