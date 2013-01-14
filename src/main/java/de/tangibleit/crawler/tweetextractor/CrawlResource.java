package de.tangibleit.crawler.tweetextractor;

import java.sql.Connection;
import java.sql.SQLException;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.Factory;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import de.tangibleit.crawler.tweetextractor.Messages.*;
import de.tangibleit.crawler.tweetextractor.db.tables.records.OrganizationRecord;
import de.tangibleit.crawler.tweetextractor.db.tables.records.QueueRecord;

import static de.tangibleit.crawler.tweetextractor.db.Tables.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class CrawlResource extends ServerResource {
	@Get
	public String represent() {
		ActorRef manager = App.MANAGER;
		manager.tell(new Messages.Update());
		return "Ok.";
	}
}
