package de.tangibleit.crawler.twitterUser;

import org.jooq.SQLDialect;
import org.jooq.impl.Factory;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import de.tangibleit.crawler.twitterUser.db.Tables;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.Queue;
import de.tangibleit.crawler.twitterUser.db.tables.records.QueueRecord;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import de.tangibleit.crawler.twitterUser.Messages.*;

public class CrawlResource extends ServerResource {
	private final ActorRef manager = ActorSystem.create().actorFor("/manager");

	@Get
	public String represent() {
		Factory create = new Factory(SQLDialect.MYSQL);
		for (QueueRecord qr : create.fetch(Tables.QUEUE)) {
			Queue q = qr.into(Queue.class);
			if (q.getIslist() == 0)
				manager.tell(new Messages.CrawlUser(q.getName()));
			else
				manager.tell(new Messages.CrawlList(q.getName(), q
						.getListowner()));
		}
		return "bla";
	}
}
