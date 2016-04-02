package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;

public class RecordingsActivity extends Activity {

    private static final String TAG = "RecordingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        // start data service
        Intent serviceIntent = new Intent(this, DataService.class);
        startService(serviceIntent);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

}
