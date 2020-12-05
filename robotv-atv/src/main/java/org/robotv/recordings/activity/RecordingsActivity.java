package org.robotv.recordings.activity;

import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import org.robotv.recordings.fragment.RecordingsFragment;
import org.robotv.robotv.R;
import org.robotv.ui.DataServiceActivity;

public class RecordingsActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recordings);

        RecordingsFragment fragment = (RecordingsFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        setServiceListener(fragment);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

}
