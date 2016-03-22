package org.xvdr.recordings.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

public class CoverSearchActivity extends Activity {

    private static final int REQUEST_SPEECH = 1;
    public static final String EXTRA_MOVIE = "extra_movie";

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
    }

    public void setArtwork(ArtworkHolder holder) {
        Connection connection = new Connection("roboTV movie artwork");
        connection.open(SetupUtils.getServer(this));

        ArtworkUtils.setMovieArtwork(connection, mMovie, holder);

        connection.close();
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
