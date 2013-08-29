package za.co.maiatoday.autoselfie.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import za.co.maiatoday.autoselfie.util.MiscUtils;
import za.co.maiatoday.autoselfie.util.SelfieStatus;


//import com.google.analytics.tracking.android.EasyTracker;


public class MainActivity extends ActionBarActivity {
    final String TAG = "MainActivity";
    //Constants

    // Update status button
    Button btnUpdateStatus;
    // EditText for update
    TextView txtUpdate;
    // lbl update
    TextView lblUpdate;

    // Progress dialog
    ProgressDialog pDialog;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    private static final int REQUEST_INFO = 1;
    private static final int REQUEST_IMAGE = 2;
    private ImageView imageView;
    Button btnSnap;

    SelfieStatus selfie = new SelfieStatus();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.result);
        btnSnap = (Button) findViewById(R.id.btnSnap);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

        // All UI elements
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        txtUpdate = (TextView) findViewById(R.id.txtUpdateStatus);
        txtUpdate.setEnabled(false);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                PREF_NAME, 0);


        /**
         * Button click event to Update Status, will call UpdateTwitterStatusTask()
         * function
         * */
        btnUpdateStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call update status function
                // Get the status from EditText
                String status = txtUpdate.getText().toString();

                // Check for blank text
                if (status.trim().length() > 0) {
                    // update status
                    new UpdateTwitterStatusTask().execute(status);
                } else {
                    // EditText is empty
                    Toast.makeText(getApplicationContext(),
                            "Please enter status message", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        btnSnap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
//                openCamera();
                openImageIntent();
            }
        });

        BitmapDrawable d = (BitmapDrawable) getResources().getDrawable(R.drawable.autoselfie_test);
        if (d != null) {
            selfie.setOrig(d.getBitmap());
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
                openInfo();
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
        btnUpdateStatus.setEnabled(isTwitterLoggedInAlready());
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_INFO:
                    boolean doLogin = data.getBooleanExtra(InfoActivity.LOG_IN_TWITTER, false);
                    if (doLogin) {
                        LogInToTwitterTask t = new LogInToTwitterTask();
                        t.execute();
                    }
                    break;
                case REQUEST_IMAGE:
                    processImage(data);

                    imageView.setImageBitmap(selfie.getBmpToPost()); //TODO only for debug to see result here
                    txtUpdate.setText(selfie.getStatus());
                    break;
            }
        }
    }

    /**
     * open the InfoActivity intent
     */
    private void openInfo() {
        Intent i = new Intent(this, InfoActivity.class);
        startActivityForResult(i, REQUEST_INFO);
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
    private Uri outputFileUri;

    /**
     * Open and intent to get an image, either from camera or gallery
     */
    private void openImageIntent() {

// Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "autoSelfie" + File.separator);
        root.mkdirs();
        final String fname = MiscUtils.getUniqueImageFilename();
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        startActivityForResult(chooserIntent, REQUEST_IMAGE);
    }

    /**
     * Process the intent data and get the image
     *
     * @param data
     */
    private void processImage(Intent data) {
        final boolean isCamera;
        if (data == null) {
            isCamera = true;
        } else {
            final String action = data.getAction();
            if (action == null) {
                isCamera = false;
            } else {
                isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }
        }

        Uri selectedImageUri;
        if (isCamera) {
            selectedImageUri = outputFileUri;
        } else {
            selectedImageUri = data == null ? null : data.getData();
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            selfie.setOrig(bitmap);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {

            Toast.makeText(getApplicationContext(),
                    "Problem loading file", Toast.LENGTH_LONG).show();
        }
    }

    //---------twitter code--------------------
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

                btnUpdateStatus.setEnabled(isTwitterLoggedInAlready());

            }
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
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
            // Clearing EditText field
            txtUpdate.setText("");
        }

    }


}
