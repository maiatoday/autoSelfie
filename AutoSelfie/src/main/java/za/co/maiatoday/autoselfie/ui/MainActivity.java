package za.co.maiatoday.autoselfie.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.preferences.Prefs;
import za.co.maiatoday.autoselfie.util.ConnectionDetector;
import za.co.maiatoday.autoselfie.util.SelfieStatus;
import za.co.maiatoday.autoselfie.util.TwitterHelper;


public class MainActivity extends ActionBarActivity implements OnTwitterRequest {
    private static final String MAIN_FRAGMENT = "main";
    private static final String INFO_FRAGMENT = "info";
    private static final String AUTH_FRAGMENT = "authDialog";
    final String TAG = "MainActivity";
    // Internet Connection detector
    private ConnectionDetector cd;
    // Progress dialog
    ProgressDialog pDialog;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;
    private TwitterHelper twitHelper;

    boolean debugNoTweet = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences(Prefs.PREF_NAME, 0);
        // Start the first fragment.
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            if (findViewById(R.id.fragment_container) != null) {
                Fragment firstFragment;
                String tag;
                firstFragment = new MainFragment();
                tag = MAIN_FRAGMENT;

                // In case this activity was started with special instructions from an Intent,
                // pass the Intent's extras to the fragment as arguments
                firstFragment.setArguments(getIntent().getExtras());

                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment, tag).commit();
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);    // Shared Preferences
        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }
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
        Log.d(TAG, "onResume");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);

    }

    @Override
    protected void onStart() {

        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {

        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "onDestroy");
        super.onDestroy();
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Add the fragment to the 'fragment_container' FrameLayout
        transaction.replace(R.id.fragment_container, newFragment, INFO_FRAGMENT);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    private void switchToMainFragment() {
        if (findViewById(R.id.fragment_container) != null) {

            FragmentManager manager = getSupportFragmentManager();
            Fragment f = manager.findFragmentByTag(MAIN_FRAGMENT);
            //only replace the fragment with main if it isn't the main fragment already
            if (f == null) {

                // Create an instance of ExampleFragment
                MainFragment newFragment = new MainFragment();

                // In case this activity was started with special instructions from an Intent,
                // pass the Intent's extras to the fragment as arguments
                newFragment.setArguments(getIntent().getExtras());
                manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction transaction = manager.beginTransaction();

                // Add the fragment to the 'fragment_container' FrameLayout
                transaction.replace(R.id.fragment_container, newFragment, MAIN_FRAGMENT);
                // Commit the transaction
                transaction.commit();
            }
        }

    }

    //-------------- opencv test for manager code ----------------
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

    //Display the oAuth web page in a dialog
    void showAuthDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        OAuthFragment newFragment = new OAuthFragment();
        newFragment.setTwitterHelper(this.twitHelper);
        newFragment.show(ft, AUTH_FRAGMENT);
    }


    /**
     * AsyncTask to update status
     */
    class LogInToTwitterTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = "";
            twitHelper = new TwitterHelper(MainActivity.this);
            if (!twitHelper.isLoggedIn()) {
                if (twitHelper.setupRequestToken()) {
                    result = "ok";
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
            } else {
                Log.d(TAG, "startActivity oauth intent");
                showAuthDialog();
            }
        }
    }


    private void tellFragmentsTwitterStatus(boolean loggedIn) {
        Log.d(TAG, "twitter logged in " + loggedIn);
        if (loggedIn) {
            switchToMainFragment();
        } else {
            switchToInfoFragment();
        }

    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    @Override
    public boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(Prefs.PREF_KEY_TWITTER_LOGIN, false);
    }

    SelfieStatus selfie;

    @Override
    public void updateStatus(SelfieStatus status) {
        selfie = status;
        if (!debugNoTweet) {
            UpdateTwitterStatusTask t = new UpdateTwitterStatusTask();
            t.execute(selfie.getStatus());
        }
    }

    @Override
    public void checkTwitterLoginState() {
        tellFragmentsTwitterStatus(isTwitterLoggedInAlready());
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
            String status = selfie.getStatus();
            Log.d("Tweet Text", "> " + status);
            Bitmap bitmap565 = selfie.getBmpToPost();
            final StatusUpdate statusUpdate = new StatusUpdate(status);
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
                twitHelper = new TwitterHelper(MainActivity.this);

                // Update status
                twitter4j.Status response = twitHelper.getTwitter().updateStatus(statusUpdate);

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
