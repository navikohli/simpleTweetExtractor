package de.tangibleit.crawler.twitterUser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import de.tangibleit.crawler.twitterUser.Messages.CrawlUser;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.Tweet;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.TweetUrl;

public class UserWorker extends Worker<Messages.CrawlUser> {
	private final String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
	private final Pattern pattern = Pattern.compile(regex);

	private String expandURL(String address) throws IOException {
		URL url = new URL(address);

		HttpURLConnection connection = (HttpURLConnection) url
				.openConnection(Proxy.NO_PROXY);
		connection.setInstanceFollowRedirects(false);
		connection.connect();
		String expandedURL = connection.getHeaderField("Location");
		connection.getInputStream().close();
		return expandedURL;
	}

	@Override
	protected void execute(CrawlUser msg) {
		log.info("Retrieve: " + msg.userName);

		// transaction.begin();

		try {
			preRequest();
			Paging paging = new Paging();
			paging.setCount(200);

			List<Status> statuses = twitter.getUserTimeline(msg.userName,
					paging);
			if (statuses.isEmpty())
				return;

			processStatuses(statuses);
			// Get total statuses count.
			User u = statuses.get(0).getUser();
			// Synchronise user
			updateUser(u);

			int statusCount = statuses.size();

			log.info("Status count: " + statuses.size());
			do {

				preRequest();
				paging.setMaxId(statuses.get(statuses.size() - 1).getId() - 1);
				statuses = twitter.getUserTimeline(msg.userName, paging);
				statusCount += statuses.size();

				processStatuses(statuses);

			} while (statuses.size() != 0);
			log.info("done!");

		} catch (TwitterException e) {
			log.info("Twitter failure: " + e.getMessage());
			// transaction.rollback();
			getContext().parent().tell(msg);
		}

		// transaction.commit();
	}

	private void processStatuses(List<Status> statuses) {

		for (Status status : statuses) {
			Tweet tweet = new Tweet();
			tweet.setMessage(status.getText());
			tweet.setTime(new Timestamp(status.getCreatedAt().getTime()));

			log.info("msg: " + status.getText());
			Matcher m = pattern.matcher(status.getText());
			while (m.find()) {
				String urlStr = m.group();
				if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
					urlStr = urlStr.substring(1, urlStr.length() - 1);
				}

				String url;
				try {
					url = expandURL(urlStr);

				} catch (IOException e) {
					// 404 or similar
					url = urlStr;
				}

				// Add url to db.
			}
		}

	}

	private void updateUser(User user) {

	}

	public String getPath() {
		return "/statuses/user_timeline";
	}

}
