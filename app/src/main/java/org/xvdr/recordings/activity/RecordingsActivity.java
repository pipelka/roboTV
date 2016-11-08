package org.xvdr.recordings.activity;

import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.fragment.RecordingsFragment;
import org.xvdr.robotv.R;
import org.xvdr.ui.DataServiceActivity;

public class RecordingsActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recordings);

        RecordingsFragment fragment = (RecordingsFragment) getFragmentManager().findFragmentById(R.id.container);
        setServiceListener(fragment);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

}
