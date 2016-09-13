package org.xvdr.robotv.service;

import android.content.Intent;
import android.media.tv.TvInputService;

public class RoboTvInputService extends TvInputService {

    @Override
    public void onCreate() {
        super.onCreate();

        // start service
        Intent serviceIntent = new Intent(this, DataService.class);
        startService(serviceIntent);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        return new RoboTvSession(this, inputId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
