package org.xvdr.timers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.xvdr.robotv.R;

public class TimerActivity extends Activity {

    private static final String TAG = "TimerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, EpgSearchActivity.class);
        startActivity(intent);
        return true;
    }

}
