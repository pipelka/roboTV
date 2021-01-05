package org.robotv.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

public class SetupUtils {

    static private final String TAG = SetupUtils.class.getName();

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

    static public TreeMap<String, String> getLanguages() {
        ArrayList<String> codeList = new ArrayList<>();
        Locale[] locales = Locale.getAvailableLocales();

        // generate list of language codes
        for(Locale locale: locales) {
            String iso3 = locale.getISO3Language();

            if(!codeList.contains(iso3)) {
                codeList.add(iso3);
            }
        }

        // assemble list of locales
        TreeMap<String, String> map = new TreeMap<>();

        for(String iso3: codeList) {
            Locale locale = new Locale(iso3);
            map.put(locale.getDisplayLanguage(), locale.getISO3Language());
        }

        return map;
    }

    static public String getDisplayLanguage(Context context) {
        TreeMap<String, String> list = getLanguages();
        String language = getLanguage(context);

        for(HashMap.Entry<String, String> entry : list.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if(value.equals(language)) {
                return key;
            }
        }

        return "";
    }

    static public String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        String defaultLang = Locale.getDefault().getISO3Language();
        String lang = prefs.getString("language", defaultLang);

        return TextUtils.isEmpty(lang) ? defaultLang : lang;
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
