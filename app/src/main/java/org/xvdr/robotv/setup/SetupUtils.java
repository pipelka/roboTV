package org.xvdr.robotv.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.xvdr.robotv.R;

import java.util.Arrays;
import java.util.Locale;

public class SetupUtils {

    static public String getServer(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString("server", "192.168.16.10");
    }

    static public void setServer(Context context, String server) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putString("server", server);
        e.apply();
    }

    static public String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString("language", Locale.getDefault().getLanguage());
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

    static public void setLanguage(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putString("language", language);
        e.apply();
    }

    public static int getRefreshRateIndex(Context context) {
        String[] array = context.getResources().getStringArray(R.array.refresh_rate_value_array);

        float[] refreshRateValueArray = new float[array.length];

        for(int i = 0; i < array.length; i++) {
            refreshRateValueArray[i] = Float.parseFloat(array[i]);
        }

        float rate = getRefreshRate(context);

        for(int i = 0; i < array.length; i++) {
            if(refreshRateValueArray[i] == getRefreshRate(context)) {
                return i;
            }
        }

        return -1;
    }

    public static boolean isRefreshRateChangeSupported() {
        return false;
    }

    public static float getRefreshRate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getFloat("refreshrate", 50f);
    }

    public static void setRefreshRate(Context context, float refreshRate) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putFloat("refreshrate", refreshRate);
        e.apply();
    }

    public static boolean getPassthrough(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("passthrough", false);
    }

    public static void setPassthrough(Context context, boolean passthrough) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("passthrough", passthrough);
        e.apply();
    }

    public static int getSpeakerConfiguration(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getInt("speakerconfig", 6);
    }

    public static void setSpeakerConfiguration(Context context, int speakerConfig) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor e = prefs.edit();
        e.putInt("speakerconfig", speakerConfig);
        e.apply();
    }
}
