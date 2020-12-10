package org.robotv.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.ProgressBarManager;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

import android.text.TextUtils;
import android.util.Log;

import org.robotv.client.model.Event;
import org.robotv.recordings.activity.DetailsActivity;
import org.robotv.recordings.activity.PlayerActivity;
import org.robotv.recordings.activity.SearchActivity;
import org.robotv.recordings.model.EpisodeTimer;
import org.robotv.recordings.model.IconAction;
import org.robotv.recordings.presenter.IconActionPresenter;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.MovieCollectionAdapter;
import org.robotv.recordings.util.BackgroundManagerTarget;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.model.Timer;
import org.robotv.dataservice.DataService;
import org.robotv.client.MovieController;
import org.robotv.dataservice.NotificationHandler;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.GlideApp;

import java.util.ArrayList;

public class RecordingsFragment extends BrowseSupportFragment implements DataService.Listener, MovieController.LoaderCallback {

    private final static String TAG = "RecordingsFragment";

    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private MovieCollectionAdapter mAdapter;
    private BackgroundManager backgroundManager;
    private BackgroundManagerTarget backgroundManagerTarget;
    private NotificationHandler notification;
    private DataService service;

    private int color_background;
    private String backgroundUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notification = new NotificationHandler(getActivity());

        setupEventListeners();
        initUI();
        prepareEntranceTransition();
    }

    private void updateBackground(String url) {
        if(TextUtils.isEmpty(url) || !url.endsWith(".jpg")) {
            backgroundManager.setColor(color_background);
            backgroundManager.clearDrawable();
            return;
        }

        backgroundUrl = url;

        GlideApp.with(this).load(url)
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
        if(backgroundManager == null) {
            backgroundManager = BackgroundManager.getInstance(getActivity());
        }

        if(!backgroundManager.isAttached()) {
            backgroundManager.attach(getActivity().getWindow());
        }

        if(backgroundManagerTarget == null) {
            backgroundManagerTarget = new BackgroundManagerTarget(backgroundManager);
        }

        updateBackground(backgroundUrl);
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
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
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

                new EditTimerFragment().startGuidedStep(
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
        });

        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if(item instanceof Event) {
                Event event = (Event) item;
                updateBackground(event.getBackgroundUrl());
            }
            else {
                updateBackground(null);
            }
        });

        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });
    }

    private void startEpgSearchActivity() {
        Intent intent = new Intent(getActivity(), org.robotv.timers.activity.EpgSearchActivity.class);
        startActivity(intent);
    }

    private void startSetupActivity() {
        Intent intent = new Intent(getActivity(), org.robotv.setup.SetupActivity.class);
        startActivity(intent);
    }

    private void playbackMovie(Movie movie) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);
        intent.putExtra(EXTRA_SHOULD_AUTO_START, true);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateBackground(backgroundUrl);
    }

    @Override
    public void onMovieCollectionUpdated(ArrayList<Movie> collection, int status) {
        if(!isAdded()) {
            return;
        }

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

                if(mAdapter == null) {
                    mAdapter = new MovieCollectionAdapter(getActivity(), service.getConnection());
                    setupPreferences(mAdapter);
                }

                if(status == MovieController.STATUS_Collection_Ready) {
                    mAdapter.loadMovies(collection);
                }

                if(getAdapter() != mAdapter) {
                    setAdapter(mAdapter);
                }

                startEntranceTransition();
                updateBackground(backgroundUrl);
                break;
        }
    }

    @Override
    public void onConnected(DataService service) {
        this.service = service;

        service.getMovieController().loadMovieCollection(this);
        loadTimers(service);
    }

    @Override
    public void onConnectionError(DataService service) {
        // no entries, add at least the setup row
        if(getAdapter() == null) {
            mAdapter = new MovieCollectionAdapter(getActivity(), service.getConnection());
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
        Log.d(TAG, "onMovieUpdate()");
        service.getMovieController().loadMovieCollection(this);
    }

    @Override
    public void onTimersUpdated(DataService service) {
        loadTimers(service);
    }

    protected void loadTimers(DataService service) {
        if(mAdapter == null) {
            return;
        }
        service.getTimerController().loadTimers(timers -> mAdapter.loadTimers(timers));
    }
}
