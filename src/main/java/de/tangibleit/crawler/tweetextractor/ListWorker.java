package de.tangibleit.crawler.tweetextractor;

import java.sql.SQLException;

import twitter4j.PagableResponseList;
import twitter4j.QueryResult;
import twitter4j.TwitterException;
import twitter4j.User;
import de.tangibleit.crawler.tweetextractor.Messages.CrawlList;
import de.tangibleit.crawler.tweetextractor.Messages.CrawlUser;
import de.tangibleit.crawler.tweetextractor.Messages.Task;
import de.tangibleit.crawler.twitterUser.db.Tables;
import de.tangibleit.crawler.twitterUser.db.tables.records.QueueRecord;
import akka.actor.UntypedActor;

public class ListWorker extends Worker<CrawlList> {

	@Override
	protected void execute(CrawlList msg) throws SQLException {
		log.info(String.format("Retrieving list \"%s\" @%s", msg.listName,
				msg.userName));
		try {
			PagableResponseList<User> members = twitter.getUserListMembers(
					msg.userName, msg.listName, -1);

			// This is a bit ... unperformant.
			String organizationName = create.select().from(Tables.ORGANIZATION)
					.where(Tables.ORGANIZATION.ID.equal(msg.organizationId))
					.fetchOne(Tables.ORGANIZATION.NAME);

			do {
				for (User u : members) {
					// Create a new query entry.
					int qId = create
							.insertInto(Tables.QUEUE, Tables.QUEUE.NAME,
									Tables.QUEUE.ORGANIZATION,
									Tables.QUEUE.STATUS)
							.values(u.getScreenName(), organizationName, 2)
							.returning(Tables.QUEUE.ID).fetch().get(0).getId();

					getContext().parent().tell(
							new CrawlUser(u.getScreenName(), qId,
									msg.organizationId), self());
				}
				members = twitter.getUserListMembers(msg.userName,
						msg.listName, members.getNextCursor());
			} while (members.hasNext());

			setQueueStatus(msg.queueId, 3);
		} catch (TwitterException e) {
			log.info(String.format("List \"%s\" @%s does not exist.",
					msg.listName, msg.userName));
			connection.rollback();
			setQueueStatus(msg.queueId, 4);
		} finally {
			connection.commit();
		}
	}

	@Override
	protected String getPath() {
		return "/lists/members";
	}

}
