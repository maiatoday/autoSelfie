package za.co.maiatoday.autoselfie;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

//import com.google.analytics.tracking.android.EasyTracker;


public class MainActivity extends Activity {
    //Constants
    /**
     *      * Register your here app https://dev.twitter.com/apps/new and get your
     *      * consumer key and secret
     *      *
     */
    static String TWITTER_CONSUMER_KEY = "";
    static String TWITTER_CONSUMER_SECRET = "";
    // Preference Constants
//    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLoggedIn";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
//    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
//    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    private static final int MAX_FACES = 5;

    // Login button
    Button btnLoginTwitter;
    // Update status button
    Button btnUpdateStatus;
    // Logout button
    Button btnLogoutTwitter;
    // EditText for update
    EditText txtUpdate;
    // lbl update
    TextView lblUpdate;
    TextView lblUserName;

    // Progress dialog
    ProgressDialog pDialog;

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;
    private AccessToken accessToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();
    private static final int REQUEST_CONTENT = 1;
    private static final int REQUEST_CAMERA = 2;
    private Bitmap bitmap;
    private ImageView imageView;
    Button btnSnap;
    Button btnGallery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.result);
        btnSnap = (Button) findViewById(R.id.btnSnap);
        btnGallery = (Button) findViewById(R.id.btnGallery);
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
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
        lblUserName = (TextView) findViewById(R.id.lblUserName);

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);

        /**
         * Twitter login button click event will call loginToTwitter() function
         * */
        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login twitter function
                loginToTwitter();

            }
        });

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

        /**
         * Button click event for logout from twitter
         * */
        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call logout twitter function
                logoutFromTwitter();
            }
        });

        btnSnap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                openCamera();
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                openGetContentIntent();
            }
        });


    }


    /**
     * Function to login twitter
     */
    private void loginToTwitter() {
        LogInToTwitterTask t = new LogInToTwitterTask();
        t.execute();
    }

    /**
     * AsyncTask to update status
     */
    class LogInToTwitterTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = "";
            if (!isTwitterLoggedInAlready()) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);
                twitter = factory.getInstance();

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
            String status = args[0];
            final StatusUpdate statusUpdate = new StatusUpdate(args[0]);
//            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.autoselfie_test);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
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

    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();

        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
        btnLogoutTwitter.setVisibility(View.GONE);
        btnUpdateStatus.setVisibility(View.GONE);
        txtUpdate.setVisibility(View.GONE);
        lblUpdate.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);

        btnLoginTwitter.setVisibility(View.VISIBLE);
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    protected void onResume() {
        super.onResume();/** This if conditions is tested once is
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
        } else {
            makeLoggedInView();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    processCameraImage(data);
                    break;
                case REQUEST_CONTENT:
                    processGalleryImage(data);
                    break;
            }
            detectFaces();
        }
    }

    /**
     * AsyncTask to update status
     */
    class GetAccessTokenTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // Get the access token
            try {
                MainActivity.this.accessToken = twitter.getOAuthAccessToken(
                        requestToken, params[0]);
            } catch (Exception e) {
                MainActivity.this.accessToken = null;
            }
            if (MainActivity.this.accessToken == null) {
                return "";
            }
            return accessToken.getToken();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!TextUtils.isEmpty(s)) {
                // Shared Preferences
                SharedPreferences.Editor e = mSharedPreferences.edit();

                // After getting access token, access token secret
                // store them in application preferences
                e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                e.putString(PREF_KEY_OAUTH_SECRET,
                        accessToken.getTokenSecret());
                // Store login status - true
                e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                e.commit(); // save changes

                Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
                makeLoggedInView();


                // Getting user details from twitter
                // For now i am getting his name only
                long userID = accessToken.getUserId();
                try {
                    User user = twitter.showUser(userID);
                    String username = user.getName();

                    // Displaying in xml ui
                    lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                } catch (Exception t) {
                    lblUserName.setText(Html.fromHtml("<b>Twitter user name error</b>"));
                }
            }
        }
    }

    private void makeLoggedInView() {
        // Hide login button
        btnLoginTwitter.setVisibility(View.GONE);

        // Show Update Twitter
        lblUpdate.setVisibility(View.VISIBLE);
        txtUpdate.setVisibility(View.VISIBLE);
        btnUpdateStatus.setVisibility(View.VISIBLE);
        btnLogoutTwitter.setVisibility(View.VISIBLE);
    }

    private void openGetContentIntent() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CONTENT);
    }

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void processCameraImage(Intent intent) {
//        setContentView(R.layout.detectlayout);

//        ((Button)findViewById(R.id.detect_face)).setOnClickListener(btnClick);

//        ImageView imageView = (ImageView)findViewById(R.id.image_view);

        bitmap = (Bitmap) intent.getExtras().get("data");

        imageView.setImageBitmap(bitmap);
    }

    private void processGalleryImage(Intent intent) {

        InputStream stream = null;
        try {
            // We need to recycle unused bitmaps
            if (bitmap != null) {
                bitmap.recycle();
            }
            stream = getContentResolver().openInputStream(intent.getData());
            bitmap = BitmapFactory.decodeStream(stream);

            imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private void detectFaces() {
        if (null != bitmap) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            FaceDetector detector = new FaceDetector(width, height, MainActivity.MAX_FACES);
            FaceDetector.Face[] faces = new FaceDetector.Face[MainActivity.MAX_FACES];

            Bitmap bitmap565 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Paint ditherPaint = new Paint();
            Paint drawPaint = new Paint();

            ditherPaint.setDither(true);
            drawPaint.setColor(Color.BLACK);
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeWidth(16);

            Canvas canvas = new Canvas();
            canvas.setBitmap(bitmap565);
            canvas.drawBitmap(bitmap, 0, 0, ditherPaint);

            int facesFound = detector.findFaces(bitmap565, faces);
            PointF midPoint = new PointF();
            float eyeDistance = 0.0f;
            float confidence = 0.0f;

            Log.i("FaceDetector", "Number of faces found: " + facesFound);

            if (facesFound > 0) {
                for (int index = 0; index < facesFound; ++index) {
                    faces[index].getMidPoint(midPoint);
                    eyeDistance = faces[index].eyesDistance();
                    confidence = faces[index].confidence();

                    Log.i("FaceDetector",
                            "Confidence: " + confidence +
                                    ", Eye distance: " + eyeDistance +
                                    ", Mid Point: (" + midPoint.x + ", " + midPoint.y + ")");

//                    canvas.drawRect((int)midPoint.x - eyeDistance ,
//                            (int)midPoint.y - eyeDistance ,
//                            (int)midPoint.x + eyeDistance,
//                            (int)midPoint.y + eyeDistance, drawPaint);
                    canvas.drawLine((int) midPoint.x - eyeDistance,
                            (int) midPoint.y,
                            (int) midPoint.x + eyeDistance,
                            (int) midPoint.y, drawPaint);
                }
            }

            String filepath = Environment.getExternalStorageDirectory() + "/facedetect" + System.currentTimeMillis() + ".jpg";

            try {
                FileOutputStream fos = new FileOutputStream(filepath);

                bitmap565.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

//            ImageView imageView = (ImageView)findViewById(R.id.image_view);

            imageView.setImageBitmap(bitmap565);
        }
    }
}
