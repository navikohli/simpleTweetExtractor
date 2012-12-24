package de.tangibleit.crawler.twitterUser;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkerManager extends UntypedActor {
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private ActorRef userWorker = getContext().actorOf(
			new Props(UserWorker.class), "user");

	@Override
	public void onReceive(Object msg) throws Exception {
		userWorker.tell(msg);
	}

}
