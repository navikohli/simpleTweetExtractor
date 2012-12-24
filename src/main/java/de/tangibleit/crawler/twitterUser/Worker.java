package de.tangibleit.crawler.twitterUser;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Seconds;
import org.jooq.SQLDialect;
import org.jooq.impl.Factory;

import de.tangibleit.crawler.twitterUser.Messages.Task;
import scala.collection.parallel.ParSeqLike.LastIndexWhere;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import akka.actor.ActorLogging;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class Worker<T extends Task> extends UntypedActor {
	protected Twitter twitter = new TwitterFactory(new ConfigurationBuilder()
			.setIncludeEntitiesEnabled(true).build()).getInstance();

	protected abstract void execute(T msg) throws SQLException;

	protected abstract String getPath();

	protected LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	protected Connection connection = null;
	protected Factory create = null;

	private Instant windowEnd = null;
	private Instant lastRequest = Instant.now();
	private int windowRemaining = 0;

	private void initialise(String path) {
		try {
			RateLimitStatus status = twitter.getRateLimitStatus(
					path.split("/")[1]).get(path);
			windowEnd = Instant.now().withDurationAdded(
					status.getSecondsUntilReset(), 1000);
			windowRemaining = status.getRemaining();

			log.info("windowEnd: " + windowEnd + " ::: windowRemaining: "
					+ windowRemaining);
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void preRequest() {

		if (windowRemaining == 0 || (windowRemaining % 5 == 0))
			initialise(getPath());

		if (windowRemaining == 0) {
			long sec = Seconds.secondsBetween(DateTime.now(), windowEnd)
					.getSeconds();
			log.info("Going to sleep for: " + sec);
			try {
				Thread.sleep((sec + 3) * 1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else
			try {
				long toSleep = Seconds.secondsBetween(lastRequest,
						DateTime.now()).getSeconds() * 1000;
				if (toSleep < 4000L) {
					toSleep = 4000L - toSleep;
					Thread.sleep(toSleep);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		lastRequest = Instant.now();

	}

	@Override
	public void onReceive(Object msg) throws Exception {
		T task = (T) msg;

		execute(task);
		getContext().parent().tell(new Messages.Idle());
	}

	@Override
	public void postStop() {
		try {
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.postStop();
	}

	@Override
	public void preStart() {
		super.preStart();
		try {
			connection = App.DATASOURCE.getConnection();
			connection.setAutoCommit(false);
			create = new Factory(connection, SQLDialect.MYSQL);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		getContext().parent().tell(new Messages.Idle());
	}
}
