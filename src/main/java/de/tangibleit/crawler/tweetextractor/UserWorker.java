package de.tangibleit.crawler.tweetextractor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import de.tangibleit.crawler.tweetextractor.Messages.CrawlUser;
import de.tangibleit.crawler.twitterUser.db.Tables;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.BlacklistUrl;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.Tweet;
import de.tangibleit.crawler.twitterUser.db.tables.pojos.TweetUrl;
import de.tangibleit.crawler.twitterUser.db.tables.records.TweetRecord;
import de.tangibleit.crawler.twitterUser.db.tables.records.TweetUrlRecord;
import de.tangibleit.crawler.twitterUser.db.tables.records.UserRecord;

public class UserWorker extends Worker<Messages.CrawlUser> {
	private final String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
	private final Pattern pattern = Pattern.compile(regex);
	private List<String> blacklist;

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
	protected void execute(CrawlUser msg) throws SQLException {
		log.info("Retrieve: " + msg.userName);
		try {
			// transaction.begin();

			updateBlacklist();

			try {
				preRequest();
				Paging paging = new Paging();
				paging.setCount(200);

				long id;
				User u = twitter.showUser(msg.userName);
				updateUser(u, msg.organizationId);
				id = u.getId();

				// If we've already crawled this user in the past, don't crawl
				// his old tweets.
				TweetRecord rec = create.selectFrom(Tables.TWEET)
						.where(Tables.TWEET.USER_ID.equal(id))
						.orderBy(Tables.TWEET.ID.desc()).limit(1).fetchOne();
				if (rec != null)
					paging.setSinceId(rec.getId());

				List<Status> statuses = twitter.getUserTimeline(msg.userName,
						paging);
				if (statuses.isEmpty())
					return;

				for (Status s : statuses)
					processStatus(s);

				int statusCount = statuses.size();

				do {

					preRequest();
					paging.setMaxId(statuses.get(statuses.size() - 1).getId() - 1);
					statuses = twitter.getUserTimeline(msg.userName, paging);
					statusCount += statuses.size();

					for (Status s : statuses)
						processStatus(s);

				} while (statuses.size() != 0);

				setQueueStatus(msg.queueId, 3);
			} catch (TwitterException e) {
				log.info("Twitter failure: " + e.getMessage());
				setQueueStatus(msg.queueId, 4);
			}

		} catch (SQLException e) {
			connection.rollback();

			log.info("user: " + msg.userName + " does not exist. skipping");
			setQueueStatus(msg.queueId, 4);
		} finally {
			connection.commit();

			log.info("Handling " + msg.userName + " done.");
		}
	}

	private void processStatus(Status status) throws SQLException {

		TweetRecord rec = create.selectFrom(Tables.TWEET)
				.where(Tables.TWEET.ID.equal(status.getId())).fetchOne();
		if (rec == null) {
			rec = new TweetRecord();
			rec.attach(create);
			rec.setId(status.getId());
		} else
			return; // If the tweet already exists, skip it this time.
		rec.setMessage(status.getText());
		rec.setTime(new Timestamp(status.getCreatedAt().getTime()));
		rec.setUserId(status.getUser().getId());

		Matcher m = pattern.matcher(status.getText());
		List<TweetUrlRecord> urls = new ArrayList<TweetUrlRecord>();
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
			if (url == null)
				url = urlStr;

			String host = urlStr;
			try {
				host = (new URL(url)).getHost();
			} catch (MalformedURLException e) {
			}

			// Check for blacklisting
			// If blacklisted, ignore this tweet.
			if (blacklist.contains(host))
				return;

			TweetUrlRecord urec = new TweetUrlRecord();
			urec.attach(create);
			urec.setTweetId(status.getId());
			urec.setUrl(url);

			// Make sure that we don't commit before eliminating any possibility
			// of blacklisting
			urls.add(urec);
		}

		for (TweetUrlRecord urec : urls)
			urec.store();
		rec.store();
	}

	private void updateBlacklist() {
		blacklist = create.selectFrom(Tables.BLACKLIST_URL).fetch(
				Tables.BLACKLIST_URL.URL);
	}

	private void updateUser(User user, int organizationId) throws SQLException {
		UserRecord rec = create.selectFrom(Tables.USER)
				.where(Tables.USER.ID.equal(user.getId())).fetchOne();

		if (rec == null) {
			rec = new UserRecord();
			rec.setId(user.getId());
			rec.attach(create);
		}

		if (organizationId != -1)
			rec.setOrganizationId(organizationId);

		rec.setScreenName(user.getScreenName());
		rec.setImageUrl(user.getProfileImageURL());

		rec.store();
	}

	public String getPath() {
		return "/statuses/user_timeline";
	}

}
