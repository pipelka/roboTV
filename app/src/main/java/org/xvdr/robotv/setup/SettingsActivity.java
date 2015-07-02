package org.xvdr.robotv.setup;

import android.app.Activity;
import android.os.Bundle;

import org.xvdr.robotv.R;

public class SettingsActivity extends Activity implements
        SettingsFragment.SettingsClickedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
    }

    /**
     * Implementation of OnSettingsClickedListener
     */
    public void onSettingsClicked() {
        setResult(Activity.RESULT_OK);
    }

}
