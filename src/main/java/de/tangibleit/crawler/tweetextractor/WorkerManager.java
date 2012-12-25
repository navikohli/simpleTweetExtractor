package de.tangibleit.crawler.tweetextractor;

import java.util.Stack;
import java.util.Vector;

import de.tangibleit.crawler.tweetextractor.Messages.*;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class WorkerManager extends UntypedActor {
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private class QueueManager<T extends Task> {
		private final ActorRef ref;
		private Stack<T> queue = new Stack<T>();

		public QueueManager(Class<? extends Actor> clazz, String name) {
			ref = getContext().actorOf(new Props(clazz), name);
		}

		// Got a new object!
		public void receive(T msg) {
			boolean idle = queue.isEmpty();
			if (!queue.contains(msg))
				queue.add(msg);

			if (idle)
				idle();

		}

		// Called when worker is idle
		public void idle() {
			if (!queue.isEmpty())
				ref.tell(queue.pop());
		}
	}

	private QueueManager<CrawlUser> userQ = new QueueManager<Messages.CrawlUser>(
			UserWorker.class, "user");
	private QueueManager<CrawlList> listQ = new QueueManager<Messages.CrawlList>(
			ListWorker.class, "list");

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CrawlUser)
			userQ.receive((CrawlUser) msg);
		else if (msg instanceof CrawlList)
			listQ.receive((CrawlList) msg);
		else if (msg instanceof Idle) {
			if (sender().path().name().equals("user"))
				userQ.idle();
			else
				listQ.idle();
		}
	}

}
