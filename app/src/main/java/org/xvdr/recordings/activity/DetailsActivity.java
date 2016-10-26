package org.xvdr.recordings.activity;

import android.app.Activity;
import android.os.Bundle;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.fragment.VideoDetailsFragment;
import org.xvdr.robotv.R;

public class DetailsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.get(this).clearMemory();
    }
}
