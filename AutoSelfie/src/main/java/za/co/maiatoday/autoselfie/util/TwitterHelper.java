package za.co.maiatoday.autoselfie.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.preferences.Prefs;

/**
 * Created by maia on 2013/09/06.
 */
public class TwitterHelper {

    private boolean loggedIn = false;
    private boolean disableTweet = false;


    /**
     *   Register your here app https://dev.twitter.com/apps/new and get your
     *   consumer key and secret
     *  
     */
    public static String TWITTER_CONSUMER_KEY = "";
    public static String TWITTER_CONSUMER_SECRET = "";
    private Twitter twitter;
    public static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
    public static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public Twitter getTwitter() {
        if (twitter == null) {
            setupTwitter();
        }
        return twitter;
    }

    // Twitter

    public void setAccessToken(String token, String secret) {
        this.accessToken = new AccessToken(token, secret);
    }

    private AccessToken accessToken;
    private RequestToken requestToken;

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public RequestToken getRequestToken() {
        return requestToken;
    }

    public TwitterHelper(Context context) {
        TWITTER_CONSUMER_KEY = context.getString(R.string.consumer_key);
        TWITTER_CONSUMER_SECRET = context.getString(R.string.consumer_secret);
        // Check if twitter keys are set
//        if (TwitterHelper.TWITTER_CONSUMER_KEY.trim().length() == 0 || TwitterHelper.TWITTER_CONSUMER_SECRET.trim().length() == 0) {
//           alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
        // stop executing code by return

        SharedPreferences sp = context.getSharedPreferences(Prefs.PREF_NAME, 0);
        // Access Token
        String access_token = sp.getString(Prefs.PREF_KEY_OAUTH_TOKEN, "");
        // Access Token Secret
        String access_token_secret = sp.getString(Prefs.PREF_KEY_OAUTH_SECRET, "");
        if (!TextUtils.isEmpty(access_token) && !TextUtils.isEmpty(access_token_secret)) {
            setupTwitter(access_token, access_token_secret);
            loggedIn = true;
        } else {
            setupTwitter();
            loggedIn = false;
        }

    }

    /**
     * setup the twitter factory with the correct key and secret configuration
     *
     * @return Twitter instance
     */
    public void setupTwitter() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();
    }

    /**
     * setup the twitter factory with the correct key and secret configuration
     *
     * @return Twitter instance
     */
    public Twitter setupTwitter(String accessToken, String accessSecret) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        setAccessToken(accessToken, accessSecret);
        twitter = factory.getInstance(this.accessToken);
        return twitter;
    }

    public boolean setupRequestToken() {
        boolean ok = false;
        if (twitter == null) {
            setupTwitter();
        }
        try {
            ok = true;
            requestToken = twitter
                .getOAuthRequestToken(TwitterHelper.TWITTER_CALLBACK_URL);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return ok;
    }


    public void setupAccessToken(String... params) {

        if (twitter == null) {
            accessToken = null;
        } else {
            try {
                accessToken = twitter.getOAuthAccessToken(requestToken, params[0]);
            } catch (TwitterException e) {
                accessToken = null;

            }
        }
    }
}
