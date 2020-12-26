package org.robotv.recordings.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.robotv.client.MovieController;
import org.robotv.client.RelatedContentExtractor;
import org.robotv.recordings.activity.CoverSearchActivity;
import org.robotv.recordings.activity.PlayerActivity;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.SortedArrayObjectAdapter;
import org.robotv.recordings.presenter.DetailsDescriptionPresenter;
import org.robotv.recordings.presenter.LatestCardPresenter;
import org.robotv.recordings.presenter.MoviePresenter;
import org.robotv.recordings.util.BackgroundManagerTarget;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.model.Event;
import org.robotv.dataservice.DataService;
import org.robotv.ui.GlideApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class VideoDetailsFragment extends DetailsSupportFragment implements DataService.Listener {

    public static final String TAG = "VideoDetailsFragment";
    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_RECID = "extra_recid";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private static final int ACTION_WATCH = 1;

    private Movie selectedMovie = null;
    private DataService service;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setOnItemViewClickedListener((OnItemViewClickedListener) (itemViewHolder, item, rowViewHolder, row) -> {
            if(item instanceof Action) {
                Action action = (Action) item;
                Movie movie = (Movie) ((DetailsOverviewRow) row).getItem();
                handleDetailActions(action, movie);
            }
            else if(item instanceof Movie) {
                playbackMovie((Movie) item);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void handleDetailActions(Action action, Movie movie) {
        if((int)action.getId() == ACTION_WATCH) {
            playbackMovie(movie);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != CoverSearchActivity.REQUEST_COVER) {
            return;
        }

        if(resultCode == Activity.RESULT_OK) {
            selectedMovie = (Movie) data.getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
            BackgroundManagerTarget.setBackground(selectedMovie.getBackgroundUrl(), getActivity());
        }
    }


    private void initDetails() {
        ClassPresenterSelector ps = new ClassPresenterSelector();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);

        FullWidthDetailsOverviewRowPresenter dorPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        // set detail background and style
        dorPresenter.setBackgroundColor(Utils.getColor(getActivity(), R.color.primary_color));
        dorPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        if(selectedMovie.isSeriesHeader()) {
            addEpisodeRows(adapter, selectedMovie);
        }
        else {
            addDetailRow(adapter, selectedMovie);
        }

        loadRelatedContent(adapter);

        setAdapter(adapter);
    }

    private void addEpisodeRows(ArrayObjectAdapter adapter, Movie movie) {
        if(service == null) {
            return;
        }

        ArrayList<Movie> collection = service.getMovieController().getMovieCollection();

        if(collection == null) {
            return;
        }

        ArrayList<Movie> episodes = new RelatedContentExtractor(collection).getSeries(movie.getTitle());

        if(episodes == null) {
            return;
        }

        Comparator<Movie> compareEpisodes = (lhs, rhs) -> {
            Event.SeasonEpisodeHolder episode1 = lhs.getSeasionEpisode();
            Event.SeasonEpisodeHolder episode2 = rhs.getSeasionEpisode();

            if(episode1.valid() && episode2.valid()) {

                if(episode1.season == episode2.season) {
                    return episode1.episode < episode2.episode ? -1 : 1;
                }

                return episode1.season < episode2.season ? -1 : 1;
            }

            return lhs.getStartTime() < rhs.getStartTime() ? -1 : 1;
        };

        episodes.sort(compareEpisodes);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new MoviePresenter(service.getConnection(), false));

        listRowAdapter.addAll(0, episodes);

        HeaderItem header = new HeaderItem(0, getString(R.string.episodes));
        adapter.add(new ListRow(header, listRowAdapter));
    }

    private void addDetailRow(ArrayObjectAdapter adapter, Movie movie) {
        final DetailsOverviewRow row = new DetailsOverviewRow(movie);

        Event.SeasonEpisodeHolder episode = movie.getSeasionEpisode();

        String url = movie.getPosterUrl();

        if(TextUtils.isEmpty(url)) {
            row.setImageDrawable(getResources().getDrawable(R.drawable.recording_unkown, null));
        }
        else {
            GlideApp.with(getActivity())
                    .load(url)
                    .override(Utils.dpToPx(R.integer.artwork_poster_width, getActivity()), Utils.dpToPx(R.integer.artwork_poster_height, getActivity()))
                    .error(getResources().getDrawable(R.drawable.recording_unkown, null))
                    .placeholder(getResources().getDrawable(R.drawable.recording_unkown, null))
                    .centerCrop()
                    .into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                            row.setImageDrawable(resource);
                        }
                    });
        }

        SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();

        actions.set(0,
                new Action(
                        ACTION_WATCH,
                        null,
                        getResources().getString(R.string.watch),
                        getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp, null)
                )
        );

        row.setActionsAdapter(actions);
        adapter.add(row);
    }

    private void playbackMovie(Movie movie) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(EXTRA_RECID, movie.getRecordingIdString());
        intent.putExtra(EXTRA_SHOULD_AUTO_START, true);
        startActivity(intent);
    }

    private void loadRelatedContent(ArrayObjectAdapter adapter) {
        // disable related content in series view
        if(selectedMovie.isTvShow()) {
            return;
        }

        if(service == null) {
            Log.d(TAG, "service is null");
            return;
        }

        ArrayList<Movie> collection = service.getMovieController().getRelatedContent(selectedMovie);

        if(collection == null) {
            return;
        }

        SortedArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                MovieController.compareTimestamps,
                new LatestCardPresenter(service.getConnection(), false));

        listRowAdapter.addAll(collection);

        HeaderItem header = new HeaderItem(0, getString(R.string.related_movies));
        adapter.add(new ListRow(header, listRowAdapter));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackgroundManager manager = BackgroundManager.getInstance(getActivity());
        manager.setAutoReleaseOnStop(false);
        manager.attach(getActivity().getWindow());

        prepareEntranceTransition();
    }

    @Override
    public void onConnected(DataService service) {
        VideoDetailsFragment.this.service = service;

        if(selectedMovie == null) {
            selectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(EXTRA_MOVIE);
        }

        if(selectedMovie.isTvShow()) {
            setTitle(selectedMovie.getTitle());
        }

        initDetails();
        startEntranceTransition();
    }

    @Override
    public void onConnectionError(DataService service) {
    }

    @Override
    public void onMovieUpdate(DataService service) {
    }

    @Override
    public void onTimersUpdated(DataService service) {
    }
}
