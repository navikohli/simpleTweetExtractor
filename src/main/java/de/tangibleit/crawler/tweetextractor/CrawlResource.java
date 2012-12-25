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
import de.tangibleit.crawler.twitterUser.db.Tables;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.Queue;
import de.tangibleit.crawler.twitterUser.db.tables.records.OrganizationRecord;
import de.tangibleit.crawler.twitterUser.db.tables.records.QueueRecord;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class CrawlResource extends ServerResource {
	@Get
	public String represent() {
		ActorRef manager = App.MANAGER;
		Connection con = null;
		try {
			con = App.DATASOURCE.getConnection();

			Factory create = new Factory(con, SQLDialect.MYSQL);

			for (QueueRecord qr : create.selectFrom(Tables.QUEUE)
					.where(Tables.QUEUE.STATUS.equal(1)).fetch()) {
				Integer oId = -1;
				if (qr.getOrganization() != null
						&& !qr.getOrganization().isEmpty()) {
					oId = create
							.selectFrom(Tables.ORGANIZATION)
							.where(Tables.ORGANIZATION.NAME.equal(qr
									.getOrganization()))
							.fetchOne(Tables.ORGANIZATION.ID);

					if (oId == null) {

						// For some weird reason, fetchOne() returns null. Bug?
						Result<OrganizationRecord> rec = create
								.insertInto(Tables.ORGANIZATION,
										Tables.ORGANIZATION.NAME)
								.values(qr.getOrganization())
								.returning(Tables.ORGANIZATION.ID).fetch();
						oId = rec.get(0).getId();
					}

				}

				if (qr.getIsList() == 0)
					manager.tell(new Messages.CrawlUser(qr.getName(), qr
							.getId(), oId));
				else
					manager.tell(new Messages.CrawlList(qr.getName(), qr
							.getListName(), qr.getId(), oId));

				qr.setStatus(2);
				qr.store();
			}

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
