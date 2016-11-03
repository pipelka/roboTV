package org.xvdr.recordings.activity;

import android.os.Bundle;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.fragment.VideoDetailsFragment;
import org.xvdr.robotv.R;
import org.xvdr.ui.DataServiceActivity;

public class DetailsActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        VideoDetailsFragment fragment = (VideoDetailsFragment) getFragmentManager().findFragmentById(R.id.details_fragment);
        setServiceListener(fragment);
    }

}
