package org.robotv.recordings.activity;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import org.robotv.robotv.R;

public class SearchActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
