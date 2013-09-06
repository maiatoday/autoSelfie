package za.co.maiatoday.autoselfie.ui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import twitter4j.User;
import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.preferences.Prefs;
import za.co.maiatoday.autoselfie.util.TwitterHelper;

/**
 * Created by maia on 2013/09/06.
 */
public class OAuthFragment extends DialogFragment {
    private String url = "";

    private WebView webViewOauth;
    private TwitterHelper twitHelper;

    public void setTwitterHelper(TwitterHelper twitHelper) {
        this.twitHelper = twitHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //check if the login was successful and the access token returned
            //this test depend of your API
            if (url.startsWith(TwitterHelper.TWITTER_CALLBACK_URL)) {
                //save your token
                saveAccessToken(url);
                return true;
            }
//            BaseActivity.logEvent(Consts.EVENT_CALLBACK + "Login Failed", true);
            return false;
        }
    }

    private void saveAccessToken(String url) {
        // extract the token if it exists
        String paths[] = url.split(TwitterHelper.URL_TWITTER_OAUTH_VERIFIER + "=");
        if (paths.length > 1) {
            new GetAccessTokenTask().execute(paths[1]);
        }
    }

    @Override
    public void onViewCreated(View arg0, Bundle arg1) {
        super.onViewCreated(arg0, arg1);
        //load the url of the oAuth login page
        webViewOauth
                .loadUrl(twitHelper.getRequestToken().getAuthenticationURL());
        //set the web client
        webViewOauth.setWebViewClient(new MyWebViewClient());
        //activates JavaScript (just in case)
        WebSettings webSettings = webViewOauth.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Retrieve the webview
        View v = inflater.inflate(R.layout.fragment_oauth, container, false);
        webViewOauth = (WebView) v.findViewById(R.id.web_oauth);
        getDialog().setTitle("Use your Twitter account");
        return v;
    }

    /**
     * AsyncTask to to get the access token
     */
    class GetAccessTokenTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected String doInBackground(String... params) {
            // Get the access token
            twitHelper.setupAccessToken(params[0]);
            if (twitHelper.getAccessToken() == null) {
                return "";
            }
            return twitHelper.getAccessToken().getToken();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!TextUtils.isEmpty(s)) {
                // Shared Preferences
                SharedPreferences.Editor e = getActivity().getSharedPreferences(Prefs.PREF_NAME, 0).edit();

                // After getting access token, access token secret
                // store them in application preferences
                e.putString(Prefs.PREF_KEY_OAUTH_TOKEN, twitHelper.getAccessToken().getToken());
                e.putString(Prefs.PREF_KEY_OAUTH_SECRET,
                        twitHelper.getAccessToken().getTokenSecret());
                // Store login status - true
                e.putBoolean(Prefs.PREF_KEY_TWITTER_LOGIN, true);
                e.commit(); // save changes

                Log.d("Twitter OAuth Token", "> " + twitHelper.getAccessToken().getToken());

                OnTwitterRequest activity = (OnTwitterRequest) getActivity();
                activity.checkTwitterLoginState();
                // Getting user details from twitter
                // For now i am getting his name only
                long userID = twitHelper.getAccessToken().getUserId();
                try {
                    User user = twitHelper.getTwitter().showUser(userID);
                    String username = user.getName();

                } catch (Exception t) {
                }

                OAuthFragment.this.dismiss();
            }
        }
    }
}
