package org.robotv.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.SearchFragment;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.SpeechRecognitionCallback;

import org.robotv.robotv.R;

public class SearchActivity extends FragmentActivity {

    private static final int REQUEST_SPEECH = 0x10;
    SearchSupportFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mFragment = (SearchSupportFragment) getSupportFragmentManager().findFragmentById(R.id.search);

        /*mFragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        });*/

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
