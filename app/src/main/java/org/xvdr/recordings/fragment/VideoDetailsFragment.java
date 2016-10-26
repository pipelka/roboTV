package org.xvdr.recordings.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.xvdr.recordings.activity.CoverSearchActivity;
import org.xvdr.recordings.activity.PlayerActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.model.RelatedContentExtractor;
import org.xvdr.recordings.model.SortedArrayObjectAdapter;
import org.xvdr.recordings.presenter.ActionPresenterSelector;
import org.xvdr.recordings.presenter.ColorAction;
import org.xvdr.recordings.presenter.DetailsDescriptionPresenter;
import org.xvdr.recordings.presenter.LatestCardPresenter;
import org.xvdr.recordings.util.BackgroundManagerTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

import java.util.Arrays;
import java.util.Collection;

public class VideoDetailsFragment extends DetailsFragment {

    public static final String TAG = "VideoDetailsFragment";
    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_EDIT = 2;
    private static final int ACTION_MOVE = 3;
    private static final int ACTION_DELETE_EPISODE = 4;
    private static final int ACTION_DELETE = 5;

    private Movie selectedMovie;
    private Collection<Movie> episodes;
    private BackgroundManager backgroundManager;
    private BackgroundManagerTarget backgroundManagerTarget;

    private DataServiceClient dataClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBackground();

        dataClient = new DataServiceClient(getActivity(), new DataServiceClient.Listener() {
            @Override
            public void onServiceConnected(DataService service) {
                initDetails();
            }

            @Override
            public void onServiceDisconnected(DataService service) {
            }

            @Override
            public void onMovieCollectionUpdated(DataService service, Collection<Movie> collection, int status) {
            }
        });
        dataClient.bind();

        setOnItemViewClickedListener(new BaseOnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Object row) {
                if(item instanceof ColorAction) {
                    handleExtraActions((ColorAction) item);
                }
                else if(item instanceof Action) {
                    Action action = (Action) item;
                    Movie movie = (Movie) ((DetailsOverviewRow) row).getItem();
                    if (action.getId() == ACTION_WATCH) {
                        playbackMovie(movie);
                    }
                    else if (action.getId() == ACTION_DELETE_EPISODE) {
                        new DeleteMovieFragment().startGuidedStep(getActivity(), movie, dataClient.getService());
                    }
                }
                else if(item instanceof Movie) {
                    playbackMovie((Movie) item);
                }
            }
        });

        selectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(EXTRA_MOVIE);

        if(selectedMovie.isSeries()) {
            setTitle(selectedMovie.getTitle());
        }
    }

    void handleExtraActions(ColorAction action) {
        switch((int)action.getId()) {
            case ACTION_EDIT:
                Intent intent = new Intent(getActivity(), CoverSearchActivity.class);
                intent.putExtra(EXTRA_MOVIE, selectedMovie);
                startActivityForResult(intent, CoverSearchActivity.REQUEST_COVER);
                return;

            case ACTION_DELETE:
                new DeleteMovieFragment().startGuidedStep(
                        getActivity(),
                        selectedMovie,
                        dataClient.getService(),
                        R.id.details_fragment);
                return;

            case ACTION_MOVE:
                new MovieFolderFragment().startGuidedStep(
                        getActivity(),
                        selectedMovie,
                        dataClient.getService(),
                        R.id.details_fragment);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != CoverSearchActivity.REQUEST_COVER) {
            return;
        }

        if(resultCode == Activity.RESULT_OK) {
            selectedMovie = (Movie) data.getSerializableExtra(VideoDetailsFragment.EXTRA_MOVIE);
            initDetails();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        dataClient.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initBackground() {
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());

        backgroundManagerTarget = new BackgroundManagerTarget(backgroundManager);

        if(selectedMovie != null && !TextUtils.isEmpty(selectedMovie.getBackgroundImageUrl())) {
            updateBackground(selectedMovie.getBackgroundImageUrl());
        }
    }

    protected void updateBackground(String url) {
        if(url == null || url.isEmpty()) {
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

        if(selectedMovie.isSeries()) {
            addEpisodeRows(adapter, selectedMovie);
        }
        else {
            addDetailRow(adapter, selectedMovie);
        }

        setExtraActions(adapter);
        loadRelatedContent(adapter);

        setAdapter(adapter);
    }

    private void addEpisodeRows(ArrayObjectAdapter adapter, Movie movie) {
        DataService service = dataClient.getService();

        if(service == null) {
            return;
        }

        Collection<Movie> collection = service.getMovieCollection();
        episodes = new RelatedContentExtractor(collection).getSeries(movie.getTitle());

        if(episodes == null) {
            return;
        }

        Movie[] movies = episodes.toArray(new Movie[episodes.size()]);
        Arrays.sort(movies, MovieCollectionAdapter.compareTimestamps);

        for (Movie m : movies) {
            addDetailRow(adapter, m);
        }
    }

    private void addDetailRow(ArrayObjectAdapter adapter, Movie movie) {
        final DetailsOverviewRow row = new DetailsOverviewRow(movie);
        row.setItem(movie);

        String url = movie.getCardImageUrl();


        if(url == null || url.isEmpty()) {
            row.setImageDrawable(getResources().getDrawable(R.drawable.recording_unkown, null));
        }

        SimpleTarget<Bitmap> target = new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                row.setImageBitmap(getActivity(), resource);
            }
        };

        Glide.with(getActivity())
                .load(url).asBitmap()
                .override(Utils.dpToPx(R.integer.artwork_poster_width, getActivity()), Utils.dpToPx(R.integer.artwork_poster_height, getActivity()))
                .error(getResources().getDrawable(R.drawable.recording_unkown, null))
                .placeholder(getResources().getDrawable(R.drawable.recording_unkown, null))
                .centerCrop()
                .into(target);

        SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();

        actions.set(0,
                new Action(
                        ACTION_WATCH,
                        null,
                        getResources().getString(R.string.watch),
                        getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp, null)
                )
        );

        if(movie.isSeries()) {
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
        getActivity().finishAndRemoveTask();
    }

    private void loadRelatedContent(ArrayObjectAdapter adapter) {
        // disable related content in series view
        if(selectedMovie.isSeries()) {
            return;
        }

        DataService service = dataClient.getService();

        if(service == null) {
            Log.d(TAG, "service is null");
            return;
        }

        Collection<Movie> collection = service.getRelatedContent(selectedMovie);

        if(collection == null) {
            return;
        }

        SortedArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(
                MovieCollectionAdapter.compareTimestamps,
                new LatestCardPresenter());

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
        if(!selectedMovie.isSeries()) {
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

    public void updateMovie(Movie movie) {
        selectedMovie = movie;
        initDetails();
    }
}
