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
		log.info(String.format("Retrieving list \"%s\" @%s", msg.listName,
				msg.userName));
		try {
			PagableResponseList<User> members = twitter.getUserListMembers(
					msg.userName, msg.listName, -1);
			do {
				for (User u : members) {
					getContext().parent().tell(
							new CrawlUser(u.getScreenName()), self());
				}
				members = twitter.getUserListMembers(msg.userName,
						msg.listName, members.getNextCursor());
			} while (members.hasNext());
		} catch (TwitterException e) {
			log.info(String.format("List \"%s\" @%s does not exist.",
					msg.listName, msg.userName));
		}
	}

	@Override
	protected String getPath() {
		return "/lists/members";
	}

}
