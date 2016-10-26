package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.fragment.RecordingsFragment;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

public class RecordingsActivity extends Activity {

    private static final String TAG = "RecordingsActivity";

    private DataServiceClient dataClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        RecordingsFragment fragment = (RecordingsFragment) getFragmentManager().findFragmentById(R.id.container);

        // start data service
        dataClient = new DataServiceClient(this, fragment);
        dataClient.bind();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataClient.unbind();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

}
