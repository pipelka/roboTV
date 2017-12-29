package org.robotv.recordings.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SectionRow;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.robotv.client.RelatedContentExtractor;
import org.robotv.recordings.activity.CoverSearchActivity;
import org.robotv.recordings.activity.PlayerActivity;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.MovieCollectionAdapter;
import org.robotv.recordings.model.SortedArrayObjectAdapter;
import org.robotv.recordings.presenter.ActionPresenterSelector;
import org.robotv.recordings.presenter.ColorAction;
import org.robotv.recordings.presenter.DetailsDescriptionPresenter;
import org.robotv.recordings.presenter.LatestCardPresenter;
import org.robotv.recordings.util.BackgroundManagerTarget;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.model.Event;
import org.robotv.dataservice.DataService;
import org.robotv.ui.MovieStepFragment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class VideoDetailsFragment extends BrowseFragment implements DataService.Listener {

    public static final String TAG = "VideoDetailsFragment";
    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_EDIT = 2;
    private static final int ACTION_MOVE = 3;
    private static final int ACTION_DELETE_EPISODE = 4;
    private static final int ACTION_DELETE = 5;

    private Movie selectedMovie = null;
    private BackgroundManagerTarget backgroundManagerTarget;
    private DataService service;
    private BackgroundManager backgroundManager;
    private MovieStepFragment actionFragment;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if(item instanceof ColorAction) {
                    handleExtraActions((ColorAction) item);
                }
                else if(item instanceof Action) {
                    Action action = (Action) item;
                    Movie movie = (Movie) ((DetailsOverviewRow) row).getItem();
                    handleDetailActions(action, movie);
                }
                else if(item instanceof Movie) {
                    playbackMovie((Movie) item);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        prepareEntranceTransition();
    }

    private void handleDetailActions(Action action, Movie movie) {
        switch((int)action.getId()) {
            case ACTION_WATCH:
                playbackMovie(movie);
                break;

            case ACTION_DELETE_EPISODE:
                actionFragment = new DeleteMovieFragment();
                actionFragment.startGuidedStep(
                        getActivity(),
                        movie,
                        service,
                        R.id.details_fragment);
                break;
        }
    }

    void handleExtraActions(ColorAction action) {
        switch((int)action.getId()) {
            case ACTION_EDIT:
                Intent intent = new Intent(getActivity(), CoverSearchActivity.class);
                intent.putExtra(EXTRA_MOVIE, selectedMovie);
                startActivityForResult(intent, CoverSearchActivity.REQUEST_COVER);
                break;

            case ACTION_DELETE:
                actionFragment = new DeleteMovieFragment();
                actionFragment.startGuidedStep(
                        getActivity(),
                        selectedMovie,
                        service,
                        R.id.details_fragment);
                break;

            case ACTION_MOVE:
                actionFragment = new MovieFolderFragment();
                actionFragment.startGuidedStep(
                        getActivity(),
                        selectedMovie,
                        service,
                        R.id.details_fragment);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != CoverSearchActivity.REQUEST_COVER) {
            return;
        }

        if(resultCode == Activity.RESULT_OK) {
            selectedMovie = (Movie) data.getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
            updateBackground(selectedMovie.getBackgroundUrl());
        }
    }


    private void initBackground() {
        if(backgroundManager == null) {
            backgroundManager = BackgroundManager.getInstance(getActivity());
        }

        if(!backgroundManager.isAttached()) {
            backgroundManager.attach(getActivity().getWindow());
        }

        backgroundManagerTarget = new BackgroundManagerTarget(backgroundManager);

        if(selectedMovie != null && !TextUtils.isEmpty(selectedMovie.getBackgroundUrl())) {
            updateBackground(selectedMovie.getBackgroundUrl());
        }
    }

    protected void updateBackground(String url) {
        int color_background = Utils.getColor(getActivity(), R.color.recordings_background);

        if(TextUtils.isEmpty(url) || !url.endsWith(".jpg")) {
            backgroundManager.setDrawable(null);
            backgroundManager.setColor(color_background);
            return;
        }

        Glide.with(getActivity())
        .load(url).asBitmap()
        .error(new ColorDrawable(Utils.getColor(getActivity(), R.color.recordings_background)))
        .into(backgroundManagerTarget);
    }

    private void initDetails() {
        ClassPresenterSelector ps = new ClassPresenterSelector();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);

        DetailsOverviewRowPresenter dorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        // set detail background and style
        dorPresenter.setBackgroundColor(Utils.getColor(getActivity(), R.color.primary_color));

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        if(selectedMovie.isSeriesHeader()) {
            addEpisodeRows(adapter, selectedMovie);
        }
        else {
            setHeadersState(HEADERS_DISABLED);
            addDetailRow(adapter, selectedMovie);
        }

        setExtraActions(adapter);
        loadRelatedContent(adapter);

        setAdapter(adapter);
    }

    private void addEpisodeRows(ArrayObjectAdapter adapter, Movie movie) {
        if(service == null) {
            return;
        }

        Collection<Movie> collection = service.getMovieController().getMovieCollection();
        Collection<Movie> episodes = new RelatedContentExtractor(collection).getSeries(movie.getTitle());

        if(episodes == null) {
            return;
        }

        Comparator<Movie> compareEpisodes = new Comparator<Movie>() {
            @Override
            public int compare(Movie lhs, Movie rhs) {
                Event.SeasonEpisodeHolder episode1 = lhs.getSeasionEpisode();
                Event.SeasonEpisodeHolder episode2 = rhs.getSeasionEpisode();

                if(episode1.valid() && episode2.valid()) {

                    if(episode1.season == episode2.season) {
                        return episode1.episode > episode2.episode ? -1 : 1;
                    }

                    return episode1.season > episode2.season ? -1 : 1;
                }

                return lhs.getStartTime() > rhs.getStartTime() ? -1 : 1;
            }
        };

        Movie[] movies = episodes.toArray(new Movie[episodes.size()]);
        Arrays.sort(movies, compareEpisodes);

        int lastSeason = -1;
        int currentSeason;

        for (Movie m : movies) {
            currentSeason = m.getSeasionEpisode().season;

            if(currentSeason != lastSeason) {
                lastSeason = currentSeason;

                if(currentSeason == 0) {
                    adapter.add(new SectionRow(new HeaderItem(getString(R.string.episodes))));
                }
                else {
                    adapter.add(new SectionRow(new HeaderItem(getString(R.string.season_nr, currentSeason))));
                }
                addDetailRow(adapter, m);
            }
            else {
                addDetailRow(adapter, m);
            }
        }
    }

    private void addDetailRow(ArrayObjectAdapter adapter, Movie movie) {
        final DetailsOverviewRow row = new DetailsOverviewRow(movie);

        Event.SeasonEpisodeHolder episode = movie.getSeasionEpisode();

        row.setHeaderItem(new HeaderItem(
                episode.valid() ? getString(R.string.episode_nr, episode.episode) :
                movie.getShortText()));
        row.setItem(movie);

        String url = movie.getPosterUrl();

        if(TextUtils.isEmpty(url)) {
            row.setImageDrawable(getResources().getDrawable(R.drawable.recording_unkown, null));
        }
        else {
            SimpleTarget<Bitmap> target = new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    if(resource != null) {
                        row.setImageBitmap(getActivity(), resource);
                    }
                }
            };

            Glide.with(getActivity())
                    .load(url).asBitmap()
                    .override(Utils.dpToPx(R.integer.artwork_poster_width, getActivity()), Utils.dpToPx(R.integer.artwork_poster_height, getActivity()))
                    .error(getResources().getDrawable(R.drawable.recording_unkown, null))
                    .placeholder(getResources().getDrawable(R.drawable.recording_unkown, null))
                    .centerCrop()
                    .into(target);
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

        if(movie.isTvShow()) {
            actions.set(1,
                    new Action(
                            ACTION_DELETE_EPISODE,
                            null,
                            getResources().getString(R.string.delete),
                            getResources().getDrawable(R.drawable.ic_delete_white_48dp, null)
                    )
            );
        }

        row.setActionsAdapter(actions);

        adapter.add(row);
    }

    private void playbackMovie(Movie movie) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);
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

        Collection<Movie> collection = service.getMovieController().getRelatedContent(selectedMovie);

        if(collection == null) {
            return;
        }

        SortedArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                MovieCollectionAdapter.compareTimestamps,
                new LatestCardPresenter(service.getConnection()));

        listRowAdapter.addAll(collection);

        HeaderItem header = new HeaderItem(0, getString(R.string.related_movies));
        adapter.add(new ListRow(header, listRowAdapter));
    }

    private void setExtraActions(ArrayObjectAdapter adapter) {
        ActionPresenterSelector presenterSelector = new ActionPresenterSelector();
        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter(presenterSelector);
        actionAdapter.add(
                new ColorAction(
                        ACTION_EDIT,
                        getResources().getString(R.string.change_cover),
                        "",
                        getResources().getDrawable(R.drawable.ic_style_white_48dp, null)
                ).setColor(Utils.getColor(getActivity(), R.color.default_background))
        );

        // disable moving to folder for series
        if(!selectedMovie.isTvShow()) {
            actionAdapter.add(
                    new ColorAction(
                            ACTION_MOVE,
                            getResources().getString(R.string.move_folder),
                            "",
                            getResources().getDrawable(R.drawable.ic_folder_white_48dp, null)
                    ).setColor(Utils.getColor(getActivity(), R.color.default_background))
            );
            actionAdapter.add(
                    new ColorAction(
                            ACTION_DELETE,
                            getResources().getString(R.string.delete),
                            "",
                            getResources().getDrawable(R.drawable.ic_delete_white_48dp, null)
                    ).setColor(Utils.getColor(getActivity(), R.color.default_background))
            );
        }

        ListRow listRow = new ListRow(new HeaderItem(getString(R.string.movie_actions)), actionAdapter);

        adapter.add(listRow);
    }

    @Override
    public void onConnected(DataService service) {
        VideoDetailsFragment.this.service = service;

        int brandColor = Utils.getColor(getActivity(), R.color.episode_header_color);
        setBrandColor(brandColor);

        if(selectedMovie == null) {
            selectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(EXTRA_MOVIE);
        }

        if(selectedMovie.isTvShow()) {
            setTitle(selectedMovie.getTitle());
        }

        initBackground();
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
