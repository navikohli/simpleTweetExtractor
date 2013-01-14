package de.tangibleit.crawler.tweetextractor;

import static de.tangibleit.crawler.tweetextractor.db.Tables.ORGANIZATION;
import static de.tangibleit.crawler.tweetextractor.db.Tables.QUEUE;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.Factory;

import de.tangibleit.crawler.tweetextractor.Messages.*;
import de.tangibleit.crawler.tweetextractor.db.Tables;
import de.tangibleit.crawler.tweetextractor.db.tables.records.OrganizationRecord;
import de.tangibleit.crawler.tweetextractor.db.tables.records.QueueRecord;
import de.tangibleit.crawler.tweetextractor.db.tables.records.TokenRecord;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkerManager extends UntypedActor {
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private HashMap<Integer, TokenRecord> lastTokens = new HashMap<Integer, TokenRecord>();

	private class QueueManager<T extends Task> {
		private final HashMap<Integer, ActorRef> refs = new HashMap<Integer, ActorRef>();
		private final HashMap<ActorRef, Boolean> idleStates = new HashMap<ActorRef, Boolean>();
		private Stack<T> queue = new Stack<T>();

		private Class<? extends Worker> clazz;
		private Constructor<? extends Worker> constructor;
		private String prefix;

		public QueueManager(Class<? extends Worker> clazz, String name) {
			this.clazz = clazz;
			this.prefix = name;

			try {
				constructor = clazz
						.getConstructor(new Class[] { TokenRecord.class });
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		public void receive(T msg) {
			if (!queue.contains(msg))
				queue.add(msg);
		}

		// Got new tasks!
		public void update() {
			for (Entry<ActorRef, Boolean> entry : idleStates.entrySet())
				if (entry.getValue())
					idle(entry.getKey());
		}

		// Called when worker is idle
		public void idle(ActorRef ref) {
			if(!refs.containsValue(ref))
				return;
			
			if (!queue.isEmpty()) {
				ref.tell(queue.pop());
				idleStates.put(ref, false);
			} else
				idleStates.put(ref, true);
		}

		public void createActors(List<TokenRecord> records) {
			for (final TokenRecord rec : records) {
				@SuppressWarnings("serial")
				ActorRef ref = context().actorOf(
						new Props(new UntypedActorFactory() {

							@Override
							public Actor create() throws Exception {
								return constructor.newInstance(rec);
							}
						}), prefix + rec.getId());
				refs.put(rec.getId(), ref);
				idleStates.put(ref, true);
			}
		}

		public void deleteActors(List<TokenRecord> records) {
			for (TokenRecord rec : records) {
				ActorRef ref = refs.get(rec.getId());
				context().stop(ref);
				refs.remove(ref);
				idleStates.remove(ref);
			}
		}
	}

	private QueueManager<CrawlUser> userQ = new QueueManager<Messages.CrawlUser>(
			UserWorker.class, "user");
	private QueueManager<CrawlList> listQ = new QueueManager<Messages.CrawlList>(
			ListWorker.class, "list");

	@Override
	public void preStart() {
		super.preStart();
		updateActors();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CrawlUser)
			userQ.receive((CrawlUser) msg);
		else if (msg instanceof CrawlList)
			listQ.receive((CrawlList) msg);
		else if (msg instanceof Idle) {
			if (sender().path().name().contains("user"))
				userQ.idle(sender());
			else
				listQ.idle(sender());
		} else if (msg instanceof Update) {
			updateActors();
			updateTasks();
		}
	}

	public void updateTasks() {
		Connection con = null;
		try {
			con = App.DATASOURCE.getConnection();

			Factory create = new Factory(con, SQLDialect.MYSQL);

			for (QueueRecord qr : create.selectFrom(QUEUE)
					.where(QUEUE.STATUS.equal(1)).fetch()) {
				Integer oId = -1;
				if (qr.getOrganization() != null
						&& !qr.getOrganization().isEmpty()) {
					oId = create
							.selectFrom(ORGANIZATION)
							.where(ORGANIZATION.NAME.equal(qr.getOrganization()))
							.fetchOne(ORGANIZATION.ID);

					if (oId == null) {

						// For some weird reason, fetchOne() returns null. Bug?
						Result<OrganizationRecord> rec = create
								.insertInto(ORGANIZATION, ORGANIZATION.NAME)
								.values(qr.getOrganization())
								.returning(ORGANIZATION.ID).fetch();
						oId = rec.get(0).getId();
					}

				}

				if (qr.getIsList() == 0)
					userQ.queue
							.add(new CrawlUser(qr.getName(), qr.getId(), oId));
				else
					listQ.queue.add(new CrawlList(qr.getName(), qr
							.getListName(), qr.getId(), oId));

				qr.setStatus(2);
				qr.store();
			}

			listQ.update();
			userQ.update();

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateActors() {
		Connection con = null;
		try {
			con = App.DATASOURCE.getConnection();
			Factory factory = new Factory(con, SQLDialect.MYSQL);
			Result<TokenRecord> tokens = factory.selectFrom(Tables.TOKEN)
					.fetch();

			HashMap<Integer, TokenRecord> tokensById = new HashMap<Integer, TokenRecord>();
			for (TokenRecord rec : tokens)
				tokensById.put(rec.getId(), rec);

			log.info("Total tokens: " + tokensById);

			List<TokenRecord> tmp = new ArrayList<TokenRecord>();
			// Find new tokens
			for (Integer id : tokensById.keySet())
				if (!lastTokens.containsKey(id))
					tmp.add(tokensById.get(id));

			userQ.createActors(tmp);
			listQ.createActors(tmp);

			log.info("New tokens: " + tmp.size());

			// Find deleted tokens
			tmp = new ArrayList<TokenRecord>();
			for (Integer id : lastTokens.keySet())
				if (!tokensById.containsKey(id))
					tmp.add(lastTokens.get(id));

			userQ.deleteActors(tmp);
			listQ.deleteActors(tmp);

			log.info("Deleted tokens: " + tmp.size());

			lastTokens = tokensById;
		} catch (Exception e) {
			// XXX
			e.printStackTrace();
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
