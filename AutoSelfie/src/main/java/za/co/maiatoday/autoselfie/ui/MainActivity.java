package za.co.maiatoday.autoselfie.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.util.ConnectionDetector;
import za.co.maiatoday.autoselfie.util.SelfieStatus;


//import com.google.analytics.tracking.android.EasyTracker;


public class MainActivity extends ActionBarActivity implements OnTwitterRequest {
    final String TAG = "MainActivity";
    // Internet Connection detector
    private ConnectionDetector cd;
    // Progress dialog
    ProgressDialog pDialog;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;
    private boolean disableTweet = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences(PREF_NAME, 0);
        // Start the first fragment.
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            if (findViewById(R.id.fragment_container) != null) {
                Fragment firstFragment;
                if (isTwitterLoggedInAlready()) {
                    firstFragment = new MainFragment();
                } else {
                    firstFragment = new InfoFragment();
                }

                // In case this activity was started with special instructions from an Intent,
                // pass the Intent's extras to the fragment as arguments
                firstFragment.setArguments(getIntent().getExtras());

                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, firstFragment).commit();
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);    // Shared Preferences

        TWITTER_CONSUMER_KEY = getString(R.string.consumer_key);
        TWITTER_CONSUMER_SECRET = getString(R.string.consumer_secret);
        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // Check if twitter keys are set
        if (TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0) {
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_info:
                switchToInfoFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
          /* This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */
        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                // oAuth verifier
                final String verifier = uri
                        .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
                new GetAccessTokenTask().execute(verifier);

            }
        }
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * open the InfoActivity intent
     */
    private void switchToInfoFragment() {
//        Intent i = new Intent(this, InfoActivity.class);
//        startActivityForResult(i, REQUEST_INFO);

        // Create fragment and give it an argument specifying the article it should show
        InfoFragment newFragment = new InfoFragment();
        Bundle args = new Bundle();
        newFragment.setArguments(args);
        switchFragment(newFragment);
    }

    private void switchToMainFragment() {
        if (findViewById(R.id.fragment_container) != null) {

            // Create an instance of ExampleFragment
            MainFragment newFragment = new MainFragment();

            // In case this activity was started with special instructions from an Intent,
            // pass the Intent's extras to the fragment as arguments
            newFragment.setArguments(getIntent().getExtras());

            switchFragment(newFragment);
        }

    }

    private void switchFragment(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Add the fragment to the 'fragment_container' FrameLayout
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();
    }


    //-------------- image code ----------------
    final private LoaderCallbackInterface mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    //---------twitter code--------------------
    @Override
    public void logInTwitter() {
        switchToMainFragment();
        LogInToTwitterTask t = new LogInToTwitterTask();
        t.execute();
    }

    /**
     *   Register your here app https://dev.twitter.com/apps/new and get your
     *   consumer key and secret
     *  
     */
    String TWITTER_CONSUMER_KEY = "";
    String TWITTER_CONSUMER_SECRET = "";
    // Preference Constants
//    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";
    static final String PREF_NAME = "autoSelfiePrefs";
    private AccessToken accessToken;
    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";


    /**
     * AsyncTask to update status
     */
    class LogInToTwitterTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = "";
            if (!isTwitterLoggedInAlready()) {
                twitter = setupTwitter();

                try {

                    requestToken = twitter
                            .getOAuthRequestToken(TWITTER_CALLBACK_URL);
                    MainActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                            .parse(requestToken.getAuthenticationURL())));
                    result = "ok";

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (TextUtils.isEmpty(s)) {
                // user already logged into twitter or error
                Toast.makeText(getApplicationContext(),
                        "Problem or already Logged into twitter", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * AsyncTask to to get the access token
     */
    class GetAccessTokenTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            twitter = setupTwitter();
        }

        @Override
        protected String doInBackground(String... params) {
            // Get the access token
            try {
                accessToken = twitter.getOAuthAccessToken(
                        requestToken, params[0]);
            } catch (Exception e) {
                accessToken = null;
            }
            if (accessToken == null) {
                return "";
            }
            return accessToken.getToken();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!TextUtils.isEmpty(s)) {
                // Shared Preferences
                SharedPreferences.Editor e = getApplicationContext().getSharedPreferences(PREF_NAME, 0).edit();

                // After getting access token, access token secret
                // store them in application preferences
                e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                e.putString(PREF_KEY_OAUTH_SECRET,
                        accessToken.getTokenSecret());
                // Store login status - true
                e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                e.commit(); // save changes

                Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

                // Getting user details from twitter
                // For now i am getting his name only
                long userID = accessToken.getUserId();
                try {
                    User user = twitter.showUser(userID);
                    String username = user.getName();

                } catch (Exception t) {
                }

            }
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    @Override
    public boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    SelfieStatus selfie;

    @Override
    public void updateStatus(SelfieStatus status) {
        selfie = status;
        if (!disableTweet) {
            UpdateTwitterStatusTask t = new UpdateTwitterStatusTask();
            t.execute(selfie.getStatus());
        }
    }

    /**
     * setup the twitter factory with the correct key and secret configuration
     *
     * @return Twitter instance
     */
    Twitter setupTwitter() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        return factory.getInstance();
    }

    /**
     * AsyncTask to update status
     */
    class UpdateTwitterStatusTask extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         */
        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = selfie.getStatus();
            Bitmap bitmap565 = selfie.getBmpToPost();
            final StatusUpdate statusUpdate = new StatusUpdate(args[0]);
//            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.autoselfie_test);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap565.compress(Bitmap.CompressFormat.PNG, 100, baos);
//            byte[] imageBytes = baos.toByteArray();
//            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
//            // then flip the stream
            byte[] myTwitterUploadBytes = baos.toByteArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(myTwitterUploadBytes);
            statusUpdate.setMedia("#autoselfie", bis);
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

                // Access Token
                String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
                // Access Token Secret
                String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // Update status
//                twitter4j.Status response = twitter.updateStatus(status);
                twitter4j.Status response = twitter.updateStatus(statusUpdate);

                Log.d("Status", "> " + response.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * *
         */
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI from Background Thread
            Toast.makeText(getApplicationContext(),
                    "Status tweeted successfully", Toast.LENGTH_SHORT)
                    .show();
        }

    }


}
