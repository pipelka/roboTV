package org.robotv.recordings.activity;

import android.os.Bundle;

import org.robotv.recordings.fragment.VideoDetailsFragment;
import org.robotv.robotv.R;
import org.robotv.ui.DataServiceActivity;

public class DetailsActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        VideoDetailsFragment fragment = (VideoDetailsFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        setServiceListener(fragment);
    }

}
