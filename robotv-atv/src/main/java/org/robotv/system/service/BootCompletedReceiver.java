package org.robotv.system.service;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.robotv.dataservice.DataService;

public class BootCompletedReceiver extends BroadcastReceiver {

    static final String TAG = BootCompletedReceiver.class.getName();

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, DataService.class);
        try {
            context.startService(serviceIntent);
        }
        catch(IllegalStateException | SecurityException e) {
            Log.d(TAG, "Failed to start service on boot - not permitted");
        }
    }
}
