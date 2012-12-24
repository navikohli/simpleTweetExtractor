package de.tangibleit.crawler.twitterUser;

import de.tangibleit.crawler.twitterUser.Messages.Task;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import akka.actor.ActorLogging;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class Worker<T extends Task> extends UntypedActor {
	public abstract int getRequestsPerWindow();

	private int remainingRequests = getRequestsPerWindow();
	protected Twitter twitter = TwitterFactory.getSingleton();

	protected abstract void execute(T msg);

	protected LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	@Override
	public void onReceive(Object msg) throws Exception {
		T task = (T) msg;

		RateLimitStatus status = twitter.getRateLimitStatus(
				task.getPath().split("/")[0]).get(task.getPath().split("/")[1]);

		log.info(
				"Status: " + status.getRemaining() + " ::: "
						+ status.getSecondsUntilReset());

		execute(task);
	}

}
