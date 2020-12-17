package org.robotv.timers.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.widget.SpeechRecognitionCallback;

import org.robotv.dataservice.DataService;
import org.robotv.timers.fragment.CreateTimerFragment;
import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.timers.fragment.EpgSearchFragment;
import org.robotv.ui.DataServiceActivity;
import org.robotv.ui.MovieStepFragment;

public class EpgSearchActivity extends DataServiceActivity {

    private static final int REQUEST_SPEECH = 1;
    private EpgSearchFragment fragment;
    private MovieStepFragment actionFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epgsearch);

        fragment = (EpgSearchFragment) getFragmentManager().findFragmentById(R.id.container);

        /*fragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(fragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        });*/

        setServiceListener(fragment);
    }

    public void selectEvent(Movie event) {
        DataService service = getService();

        if(service == null) {
            // TODO - handle error case
            return;
        }

        actionFragment = new CreateTimerFragment();
        actionFragment.startGuidedStep(
                this,
                event,
                service,
                R.id.container
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            fragment.setSearchQuery(data, true);
        }
    }

    @Override
    public boolean onSearchRequested() {
        fragment.startRecognition();
        return true;
    }
}
