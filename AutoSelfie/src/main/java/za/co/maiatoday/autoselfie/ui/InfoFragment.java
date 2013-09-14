package za.co.maiatoday.autoselfie.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import za.co.maiatoday.autoselfie.R;
import za.co.maiatoday.autoselfie.preferences.Prefs;

/**
 * Created by maia on 2013/09/01.
 */
public class InfoFragment extends Fragment {
    // Login button
    Button btnLoginTwitter;
    // Logout button
    Button btnLogoutTwitter;
    TextView infoText;
    TextView logInPrompt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        btnLoginTwitter = (Button) view.findViewById(R.id.btnLoginTwitter);
        btnLogoutTwitter = (Button) view.findViewById(R.id.btnLogoutTwitter);
        infoText = (TextView) view.findViewById(R.id.lblInfo);
        logInPrompt = (TextView) view.findViewById(R.id.tvTwitterPrompt);

        Button btnMoreInfo = (Button) view.findViewById(R.id.btnGotoWeb);
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

        infoText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/search?q=%23autoselfie&s=typd&f=realtime")));
            }
        });

        btnMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.maiatoday.co.za")));
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
        return getActivity().getSharedPreferences(Prefs.PREF_NAME, 0).getBoolean(Prefs.PREF_KEY_TWITTER_LOGIN, false);
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
        SharedPreferences.Editor e = getActivity().getSharedPreferences(Prefs.PREF_NAME, 0).edit();
        e.remove(Prefs.PREF_KEY_OAUTH_TOKEN);
        e.remove(Prefs.PREF_KEY_OAUTH_SECRET);
        e.remove(Prefs.PREF_KEY_TWITTER_LOGIN);
        e.commit();
        setButtonsView();
    }


    private void setButtonsView() {
        // Hide buttons
        boolean isLoggedIn = isTwitterLoggedInAlready();
        btnLoginTwitter.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        logInPrompt.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        btnLogoutTwitter.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }
}
