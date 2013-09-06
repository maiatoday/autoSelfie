package za.co.maiatoday.autoselfie.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import za.co.maiatoday.autoselfie.R;

import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_OAUTH_SECRET;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_OAUTH_TOKEN;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_KEY_TWITTER_LOGIN;
import static za.co.maiatoday.autoselfie.ui.MainActivity.PREF_NAME;

/**
 * Created by maia on 2013/09/01.
 */
public class InfoFragment extends Fragment implements OnTwitterLoginChanged {
    // Login button
    Button btnLoginTwitter;
    // Logout button
    Button btnLogoutTwitter;
    TextView lblUserName;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        btnLoginTwitter = (Button) view.findViewById(R.id.btnLoginTwitter);
        btnLogoutTwitter = (Button) view.findViewById(R.id.btnLogoutTwitter);
        lblUserName = (TextView) view.findViewById(R.id.lblUserName);

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setButtonsView();
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return getActivity().getSharedPreferences(PREF_NAME, 0).getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    /**
     * Function to login twitter
     */
    private void loginToTwitter() {
//        Intent i = new Intent();
//        i.putExtra(LOG_IN_TWITTER, true);
//        setResult(RESULT_OK, i);
//        finish();
        OnTwitterRequest activity = (OnTwitterRequest) getActivity();
        activity.logInTwitter();
    }

    /**
     * Function to logout from twitter
     * It will just clear the application shared preferences
     */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = getActivity().getSharedPreferences(PREF_NAME, 0).edit();
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

    @Override
    public void twitterLoggedIn(boolean loggedIn) {
        setButtonsView();
    }
}
