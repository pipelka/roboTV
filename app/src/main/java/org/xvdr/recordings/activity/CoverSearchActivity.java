package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import org.xvdr.recordings.fragment.CoverSearchFragment;
import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

import java.util.Collection;

public class CoverSearchActivity extends Activity {

    private static final int REQUEST_SPEECH = 1;
    public static final String EXTRA_MOVIE = "extra_movie";

    SearchFragment mFragment;
    DataService service;
    Movie mMovie;
    private DataServiceClient dataClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        // start data service
        dataClient = new DataServiceClient(this, new DataServiceClient.Listener() {

            @Override
            public void onServiceConnected(DataService service) {
                CoverSearchActivity.this.service = service;
            }

            @Override
            public void onServiceDisconnected(DataService service) {
                CoverSearchActivity.this.service = null;
            }

            @Override
            public void onMovieCollectionUpdated(DataService service, Collection<Movie> collection, int status) {
            }
        });

        dataClient.bind();

        mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.search);
        mFragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        });

        mMovie = (Movie) getIntent().getSerializableExtra(EXTRA_MOVIE);
        mFragment.setSearchQuery(mMovie.getTitle(), false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataClient.unbind();
    }

    public void setArtwork(ArtworkHolder holder) {
        if(service != null) {
            service.setMovieArtwork(mMovie, holder);
        }

        finishAndRemoveTask();
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
