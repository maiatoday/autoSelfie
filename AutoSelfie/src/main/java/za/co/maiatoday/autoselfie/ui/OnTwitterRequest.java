package za.co.maiatoday.autoselfie.ui;

import za.co.maiatoday.autoselfie.util.SelfieStatus;

/**
 * Created by maia on 2013/09/01.
 */

public interface OnTwitterRequest {
    void logInTwitter();

    boolean isTwitterLoggedInAlready();

    void updateStatus(SelfieStatus status);

}
