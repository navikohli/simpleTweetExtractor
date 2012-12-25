package de.tangibleit.crawler.twitterUser;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import akka.actor.ActorSystem;

public class CrawlResource extends ServerResource {
	@Get
	public String represent() {
		return "bla";
	}
}
