package org.robotv.system.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.robotv.dataservice.DataService;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, DataService.class);
        context.startService(serviceIntent);
    }

}
