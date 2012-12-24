package de.tangibleit.crawler.twitterUser;

import java.sql.Connection;
import java.sql.SQLException;

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
	@Get
	public String represent() {
		ActorRef manager = App.MANAGER;
		Connection con = null;
		try {
			con = App.DATASOURCE.getConnection();

			Factory create = new Factory(con, SQLDialect.MYSQL);
			for (QueueRecord qr : create.fetch(Tables.QUEUE)) {
				if (qr.getIslist() == 0)
					manager.tell(new Messages.CrawlUser(qr.getName()));
				else
					manager.tell(new Messages.CrawlList(qr.getName(), qr
							.getListOwner()));
			}

			create.delete(Tables.QUEUE).execute();

			return "Ok.";
		} catch (SQLException e) {
			e.printStackTrace();
			return "Database error.";
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
