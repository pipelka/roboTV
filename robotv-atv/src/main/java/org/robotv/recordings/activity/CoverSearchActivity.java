package org.robotv.recordings.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.SearchSupportFragment;

import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.dataservice.DataService;
import org.robotv.ui.DataServiceActivity;

public class CoverSearchActivity extends DataServiceActivity {

    public static final String EXTRA_MOVIE = "extra_movie";
    public static final int REQUEST_COVER = 101;

    SearchSupportFragment mFragment;
    Movie mMovie;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);

        mFragment = (SearchSupportFragment) getSupportFragmentManager().findFragmentById(R.id.container);

        mMovie = (Movie) getIntent().getSerializableExtra(EXTRA_MOVIE);
        mFragment.setSearchQuery(mMovie.getTitle(), true);

        setResult(RESULT_CANCELED, null);
    }

    public void setArtwork(ArtworkHolder holder) {
        DataService service = getService();

        if(service != null) {
            service.getMovieController().setMovieArtwork(mMovie, holder);
            service.updateRecommendationChannel();
        }

        if(holder != null) {
            mMovie.setArtwork(holder.getPosterUrl(), holder.getBackgroundUrl());
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_MOVIE, mMovie);

        setResult(RESULT_OK, intent);
        finish();
    }
}
