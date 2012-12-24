package de.tangibleit.crawler.twitterUser;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import com.jolbox.bonecp.BoneCPDataSource;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * Hello world!
 * 
 */
public class App extends Application {
	public static ActorSystem SYSTEM;
	public static ActorRef MANAGER;
	public static BoneCPDataSource DATASOURCE;

	public App() {
		super();

		SYSTEM = ActorSystem.create();
		MANAGER = SYSTEM.actorOf(new Props(WorkerManager.class), "manager");

		setupDB();
	}

	private void setupDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		DATASOURCE = new BoneCPDataSource(); // create a new datasource object
		DATASOURCE.setJdbcUrl("jdbc:mysql://localhost:3306/Crawler"); // set the
																		// JDBC
		// url
		DATASOURCE.setUsername("root"); // set the username
		DATASOURCE.setPassword(""); // set the password

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
