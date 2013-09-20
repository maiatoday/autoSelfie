package za.co.maiatoday.autoselfie.util;

/**
 * Created by maia on 2013/09/20.
 */
public class MathsUtils {

    /**
     * Make sure a value is constrained between an upper an lower bound. The int version.
     * Utility to provide constrain functionality commonly found in processing.
     *
     * @param amount
     * @param min    lower bounds
     * @param max    upper bounds
     * @return
     */
    public static int constrain(int amount, int min, int max) {
        return Math.max(min, Math.min(amount, max));
    }

    /**
     * Make sure a value is constrained between an upper an lower bound. The float version.
     * Utility to provide constrain functionality commonly found in processing.
     *
     * @param amount
     * @param min    lower bounds
     * @param max    upper bounds
     * @return
     */
    public static float constrain(float amount, float min, float max) {
        return Math.max(min, Math.min(amount, max));
    }
}
