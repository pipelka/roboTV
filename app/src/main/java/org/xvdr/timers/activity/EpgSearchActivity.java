package org.xvdr.timers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import org.xvdr.recordings.fragment.MovieStepFragment;
import org.xvdr.timers.fragment.CreateTimerFragment;
import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;

public class EpgSearchActivity extends Activity {

    private static final int REQUEST_SPEECH = 1;
    SearchFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epgsearch);

        mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.container);

        mFragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        });
    }

    public void selectEvent(Movie event) {
        new CreateTimerFragment().startGuidedStep(this, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            mFragment.setSearchQuery(data, true);
        }
    }

    @Override
    public boolean onSearchRequested() {
        mFragment.startRecognition();
        return true;
    }
}
