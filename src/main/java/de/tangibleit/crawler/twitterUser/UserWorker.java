package de.tangibleit.crawler.twitterUser;

import de.tangibleit.crawler.twitterUser.Messages.CrawlUser;

public class UserWorker extends Worker<Messages.CrawlUser> {

	@Override
	public int getRequestsPerWindow() {
		return 180;
	}

	@Override
	protected void execute(CrawlUser msg) {
		log.info("Execute");
		log.info("Retrieve: " + msg.userName);
	}

}
