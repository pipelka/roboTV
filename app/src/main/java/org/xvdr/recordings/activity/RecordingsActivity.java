package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.xvdr.robotv.R;

public class RecordingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

}
