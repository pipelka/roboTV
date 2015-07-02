package org.xvdr.robotv.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    static public boolean getUseNameAsDisplayNumber(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean("usechannelnames", true);
    }
}
