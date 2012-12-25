package de.tangibleit.crawler.twitterUser;

import java.sql.Connection;
import java.sql.DriverManager;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import akka.actor.ActorSystem;

/**
 * Hello world!
 * 
 */
public class App extends Application {
	private ActorSystem system;

	public App() {
		super();

		system = ActorSystem.create();
		System.out.println(system.toString());
	}

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());

		// Defines only one route
		router.attach("/crawl", CrawlResource.class);

		return router;
	}

	public static void main(String[] args) {
		try {
			// Create a new Component.
			Component component = new Component();

			// Add a new HTTP server listening on port 8182.
			component.getServers().add(Protocol.HTTP, 8182);

			// Attach the sample application.
			component.getDefaultHost().attach(new App());

			// Start the component.
			component.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
