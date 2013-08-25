package za.co.maiatoday.autoselfie.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.util.ConnectionDetector;

import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_OAUTH_SECRET;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_OAUTH_TOKEN;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_TWITTER_LOGIN;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_NAME;

/**
 * Created by maia on 2013/08/22.
 */
public class InfoActivity extends ActionBarActivity {

    public static final String LOG_IN_TWITTER = "logInTwitter";
    // Internet Connection detector
    private ConnectionDetector cd;

    AlertDialogManager alert = new AlertDialogManager();

    // Login button
    Button btnLoginTwitter;
    // Logout button
    Button btnLogoutTwitter;
    TextView lblUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(InfoActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        lblUserName = (TextView) findViewById(R.id.lblUserName);

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
         * Button click event for logout from twitter
         * */
        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call logout twitter function
                logoutFromTwitter();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setButtonsView();
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return getApplicationContext().getSharedPreferences(PREF_NAME, 0).getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    /**
     * Function to login twitter
     */
    private void loginToTwitter() {
        Intent i = new Intent();
        i.putExtra(LOG_IN_TWITTER, true);
        setResult(RESULT_OK, i);
        finish();
    }

    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = getApplicationContext().getSharedPreferences(PREF_NAME, 0).edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();
        setButtonsView();
    }


    private void setButtonsView() {
        // Hide buttons
        boolean isLoggedIn = isTwitterLoggedInAlready();
        btnLoginTwitter.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        btnLogoutTwitter.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }


}
