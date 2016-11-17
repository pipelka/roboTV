package org.xvdr.recordings.fragment;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.ProgressBarManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;

import android.support.v17.leanback.widget.Row;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.activity.DetailsActivity;
import org.xvdr.recordings.activity.SearchActivity;
import org.xvdr.recordings.model.EpisodeTimer;
import org.xvdr.recordings.model.IconAction;
import org.xvdr.recordings.presenter.IconActionPresenter;
import org.xvdr.robotv.client.TimerController;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.util.BackgroundManagerTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Timer;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.client.MovieController;
import org.xvdr.robotv.service.NotificationHandler;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.Collection;

public class RecordingsFragment extends BrowseFragment implements DataService.Listener, MovieController.LoaderCallback, TimerController.LoaderCallback {

    private final static String TAG = "RecordingsFragment";

    private MovieCollectionAdapter mAdapter;
    private BackgroundManager backgroundManager;
    private BackgroundManagerTarget backgroundManagerTarget;
    private NotificationHandler notification;
    private DataService service;

    private int color_background;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notification = new NotificationHandler(getActivity());
        mAdapter = new MovieCollectionAdapter(getActivity());

        setupEventListeners();
        initUI();
        prepareEntranceTransition();
    }

    private void updateBackground(String url) {
        if(TextUtils.isEmpty(url) || !url.endsWith(".jpg")) {
            backgroundManager.setDrawable(null);
            backgroundManager.setColor(color_background);
            return;
        }

        Glide.with(this).load(url).asBitmap()
            .error(new ColorDrawable(Utils.getColor(getActivity(), R.color.recordings_background)))
            .into(backgroundManagerTarget);
    }

    private void setupPreferences(MovieCollectionAdapter adapter) {
        if(adapter == null) {
            return;
        }

        HeaderItem gridHeader = new HeaderItem(1000, getActivity().getString(R.string.recordings_settings_title));
        ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new IconActionPresenter(250, 220));

        rowAdapter.add(new IconAction(
                101,
                R.drawable.ic_settings_white_48dp,
                getString(R.string.recordings_setup_title)));

        adapter.add(new ListRow(gridHeader, rowAdapter));
    }

    private void setBackground() {
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setColor(color_background);
        backgroundManager.setDimLayer(new ColorDrawable(Utils.getColor(getActivity(), R.color.dim_background)));

        backgroundManagerTarget = new BackgroundManagerTarget(backgroundManager);
    }

    private void initUI() {
        setTitle(getString(R.string.browse_title));

        //Back button goes to the fast lane, rather than home screen
        setHeadersTransitionOnBackEnabled(true);

        color_background = Utils.getColor(getActivity(), R.color.recordings_background);
        int color_brand = Utils.getColor(getActivity(), R.color.primary_color);

        setBrandColor(color_brand);
        setSearchAffordanceColor(Utils.getColor(getActivity(), R.color.recordings_search_button_color));
        setBackground();
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if(item instanceof Movie) {
                    Movie movie = (Movie) item;

                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
                    startActivity(intent);
                }
                else if(item instanceof EpisodeTimer) {
                    // TODO - add search timer handling
                }
                else if(item instanceof Timer) {
                    Timer timer = (Timer) item;

                    new DeleteTimerFragment().startGuidedStep(
                            getActivity(),
                            timer,
                            service,
                            R.id.container);
                }
                else if(item instanceof IconAction) {
                    IconAction action = (IconAction) item;
                    if(action.getActionId() == 100) {
                        startEpgSearchActivity();
                    }
                    if(action.getActionId() == 101) {
                        startSetupActivity();
                    }
                }
            }
        });

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if(item instanceof Movie) {
                    Movie movie = (Movie) item;
                    updateBackground(movie.getBackgroundUrl());
                }
                else if(item instanceof Timer) {
                    Timer timer = (Timer) item;
                    updateBackground(timer.getPosterUrl());
                }
            }
        });

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startEpgSearchActivity() {
        Intent intent = new Intent(getActivity(), org.xvdr.timers.activity.EpgSearchActivity.class);
        startActivity(intent);
    }

    private void startSetupActivity() {
        Intent intent = new Intent(getActivity(), org.xvdr.robotv.setup.SetupActivity.class);
        startActivity(intent);
    }

    @Override
    public void onMovieCollectionUpdated(Collection<Movie> collection, int status) {
        Log.d(TAG, "onMovieCollectionUpdated status=" + status);

        ProgressBarManager manager = getProgressBarManager();

        switch(status) {
            case MovieController.STATUS_Collection_Busy:
                manager.enableProgressBar();
                manager.show();
                break;
            case MovieController.STATUS_Collection_Error:
                notification.error(getString(R.string.fail_to_load_movielist));
            case MovieController.STATUS_Collection_Ready:
                manager.disableProgressBar();
                manager.hide();

                if(status == MovieController.STATUS_Collection_Ready) {
                    mAdapter.loadMovies(collection);
                }

                if(getAdapter() == null) {
                    setupPreferences(mAdapter);
                    setAdapter(mAdapter);
                }

                startEntranceTransition();
                break;
        }
    }

    @Override
    public void onTimersUpdated(Collection<Timer> timers) {
        mAdapter.loadTimers(timers);
    }

    @Override
    public void onConnected(DataService service) {
        this.service = service;

        service.getMovieController().loadMovieCollection(this);
        service.getTimerController().loadTimers(this);
    }

    @Override
    public void onConnectionError(DataService service) {
        // no entries, add at least the setup row
        if(getAdapter() == null) {
            setupPreferences(mAdapter);
            setAdapter(mAdapter);
            startEntranceTransition();
        }

        // missing setup -> start setup activity
        if(TextUtils.isEmpty(SetupUtils.getServer(getActivity()))) {
            startSetupActivity();
        }
    }

    @Override
    public void onMovieUpdate(DataService service) {
        service.getMovieController().loadMovieCollection(this);
        service.getTimerController().loadTimers(this);
    }

    @Override
    public void onTimersUpdated(DataService service) {
        service.getTimerController().loadTimers(this);
    }

}
