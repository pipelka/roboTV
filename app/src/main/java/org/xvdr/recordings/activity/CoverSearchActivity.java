package org.xvdr.recordings.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import org.robotv.client.model.Movie;
import org.xvdr.robotv.R;
import org.robotv.client.artwork.ArtworkHolder;
import org.xvdr.robotv.service.DataService;
import org.xvdr.ui.DataServiceActivity;

public class CoverSearchActivity extends DataServiceActivity {

    private static final int REQUEST_SPEECH = 1;
    public static final String EXTRA_MOVIE = "extra_movie";
    public static final int REQUEST_COVER = 101;

    SearchFragment mFragment;
    Movie mMovie;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.search);
        mFragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        });

        mMovie = (Movie) getIntent().getSerializableExtra(EXTRA_MOVIE);
        mFragment.setSearchQuery(mMovie.getTitle(), false);

        setResult(RESULT_CANCELED, null);
    }

    public void setArtwork(ArtworkHolder holder) {
        DataService service = getService();

        if(service != null) {
            service.getMovieController().setMovieArtwork(mMovie, holder);
        }

        if(holder != null) {
            mMovie.setArtwork(holder.getPosterUrl(), holder.getBackgroundUrl());
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_MOVIE, mMovie);

        setResult(RESULT_OK, intent);
        finish();
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
