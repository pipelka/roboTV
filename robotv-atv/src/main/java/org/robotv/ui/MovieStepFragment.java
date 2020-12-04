package org.robotv.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import android.text.TextUtils;

import org.robotv.client.model.Movie;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.client.MovieController;

import static org.robotv.robotv.R.drawable.recording_unkown;

public class MovieStepFragment extends GuidedStepFragment {

    static public final String EXTRA_MOVIE = "extra_movie";

    private Movie movie;
    private Drawable drawable;
    private DataService service;
    private int resourceId;

    private void getArtwork(Activity activity, String url) {
        try {
            drawable = GlideApp.with(activity)
                    .load(url)
                    .error(recording_unkown)
                    .submit(
                            Utils.dp(R.integer.artwork_background_width, activity),
                            Utils.dp(R.integer.artwork_background_height, activity)).get();
        }
        catch(Exception e) {
            e.printStackTrace();
            drawable = activity.getDrawable(recording_unkown);
        }
    }

    public void startGuidedStep(final Activity activity, final Movie movie, DataService service, final int resourceId) {
        this.service = service;
        this.resourceId = resourceId;

        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_MOVIE, movie);
        setArguments(bundle);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String url = movie.getPosterUrl();
                if(TextUtils.isEmpty(url)) {
                    url = movie.getBackgroundUrl();
                }

                getArtwork(activity, url);
                return null;
            }

            protected void onPostExecute(Void result) {
                if(resourceId != android.R.id.content) {
                    GuidedStepFragment.add(
                            activity.getFragmentManager(),
                            MovieStepFragment.this,
                            resourceId);
                    return;
                }
                GuidedStepFragment.addAsRoot(
                        activity,
                        MovieStepFragment.this,
                        resourceId);
            }
        };

        task.execute();
    }

    public Movie getMovie() {
        return movie;
    }

    protected String onCreateDescription(Movie movie) {
        return movie.getShortText();
    }

    protected GuidanceStylist.Guidance createGuidance(String breadCrumb) {
        movie = (Movie) getArguments().getSerializable(EXTRA_MOVIE);
        return createGuidance(breadCrumb, onCreateDescription(movie));
    }

    protected GuidanceStylist.Guidance createGuidance(String breadCrumb, String description) {
        movie = (Movie) getArguments().getSerializable(EXTRA_MOVIE);

        if(movie == null) {
            return new GuidanceStylist.Guidance(
                    "",
                    description,
                    breadCrumb,
                    drawable);
        }

        return new GuidanceStylist.Guidance(
                movie.getTitle(),
                description,
                breadCrumb,
                drawable);
    }

    public DataService getService() {
        return service;
    }

    public MovieController getMovieController() {
        return service.getMovieController();
    }


    public int getResourceId() {
        return resourceId;
    }

    protected Drawable getDrawable() {
        return drawable;
    }
}
