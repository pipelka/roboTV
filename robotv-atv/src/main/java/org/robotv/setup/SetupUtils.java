package org.robotv.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.robotv.robotv.R;

import java.util.Locale;

public class SetupUtils {

    static private String mRecordingFolder = "";

    static public String getServer(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString("server", "");
    }

    static void setServer(Context context, String server) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putString("server", server);
        e.apply();
    }

    static public String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString("language", "");
    }

    static public String getLanguageISO3(Context context) {
        return new Locale(getLanguage(context)).getISO3Language();
    }

    static int getLanguageIndex(Context context) {
        String[] array = context.getResources().getStringArray(R.array.iso639_code1);
        String lang = getLanguage(context);

        for(int i = 0; i < array.length; i++) {
            if(array[i].equals(lang)) {
                return i;
            }
        }

        return -1;
    }

    static void setLanguage(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putString("language", language);
        e.apply();
    }

    public static boolean getPassthrough(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("passthrough", false);
    }

    static void setPassthrough(Context context, boolean passthrough) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("passthrough", passthrough);
        e.apply();
    }

    public static void setRecordingFolder(String folder) {
        mRecordingFolder = folder;
    }

    public static String getRecordingFolder() {
        return mRecordingFolder;
    }

    public static boolean getTimeshiftEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("timeshift", true);
    }

    static void setTimeshiftEnabled(Context context, boolean timeshift) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("timeshift", timeshift);
        e.apply();
    }

    static boolean getShieldWorkaroundEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("shieldworkaround", true);
    }

    static void setShieldWorkaroundEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("shieldworkaround", enabled);
        e.apply();
    }

    static void setInputId(Context context, String inputId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putString("inputId", inputId);
        e.apply();

    }

    public static String getInputId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString("inputId", null);
    }

    public static boolean getTunneledVideoPlaybackEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("videotunneledplayback", false);
    }

    public static void setTunneledVideoPlaybackEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("videotunneledplayback", enabled);
        e.apply();
    }

    public static void setHomescreenChannelId(Context context, long channelId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putLong("homescreenChannelId", channelId);
        e.apply();
    }

    public static long getHomescreenChannelId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getLong("homescreenChannelId", -1);
    }
}
