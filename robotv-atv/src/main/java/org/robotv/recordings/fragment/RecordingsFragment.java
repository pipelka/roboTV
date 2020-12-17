package org.robotv.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.ProgressBarManager;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SearchOrbView;

import android.text.TextUtils;
import android.util.Log;

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

import java.util.ArrayList;

public class RecordingsFragment extends BrowseSupportFragment implements DataService.Listener, MovieController.LoaderCallback {

    private final static String TAG = "RecordingsFragment";

    public static final String EXTRA_MOVIE = "extra_movie";
    public static final String EXTRA_SHOULD_AUTO_START = "extra_should_auto_start";

    private NotificationHandler notification;
    private DataService service;
    private boolean create = false;
    private BackgroundManager backgroundManager;

    private MovieCollectionAdapter loadingAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        create = true;

        prepareEntranceTransition();
        notification = new NotificationHandler(getActivity());

        ProgressBarManager manager = getProgressBarManager();
        manager.enableProgressBar();
        manager.show();

        setupEventListeners();
        initUI();

        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setAutoReleaseOnStop(false);
        BackgroundManagerTarget.setBackground(null, getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void setupPreferences(ArrayObjectAdapter adapter) {
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

    private void initUI() {
        setTitle(getString(R.string.recordings));

        //Back button goes to the fast lane, rather than home screen
        setHeadersTransitionOnBackEnabled(true);

        //color_background = Utils.getColor(getActivity(), R.color.recordings_background);
        int color_brand = Utils.getColor(getActivity(), R.color.primary_color);
        setBrandColor(color_brand);

        /*setSearchAffordanceColors(new SearchOrbView.Colors(
                Utils.getColor(getActivity(), R.color.recordings_search_button_color),
                Utils.getColor(getActivity(), R.color.recordings_search_button_bright_color),
                Utils.getColor(getActivity(), R.color.recordings_search_button_icon_color)
        ));*/
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
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        backgroundManager.release();
        super.onDestroy();
    }

    @Override
    public void onMovieCollectionUpdated(ArrayList<Movie> collection, int status) {
        if(!isAdded()) {
            return;
        }

        ProgressBarManager manager = getProgressBarManager();

        switch(status) {
            case MovieController.STATUS_Collection_Busy:
                break;
            case MovieController.STATUS_Collection_Error:
                notification.error(getString(R.string.fail_to_load_movielist));
                manager.disableProgressBar();
                manager.hide();

                Log.d(TAG, "startEntranceTransition");
                startEntranceTransition();
            case MovieController.STATUS_Collection_Ready:
                Log.d(TAG, "STATUS_Collection_Ready");

                MovieCollectionAdapter adapter = createAdapter();

                if(status == MovieController.STATUS_Collection_Ready) {
                    adapter.loadMovies(collection);
                }

                if(getAdapter() == null) {
                    setAdapter(adapter);
                }

                if(create) {
                    Log.d(TAG, "startEntranceTransition");
                    manager.disableProgressBar();
                    manager.hide();
                    startEntranceTransition();
                    create = false;
                }
                else {
                    RowPresenter.ViewHolder holder = getSelectedRowViewHolder();

                    if(holder != null) {
                        Object item = holder.getSelectedItem();
                        if(item instanceof Movie) {
                            Movie movie = (Movie)item;
                            BackgroundManagerTarget.setBackground(movie.getBackgroundUrl(), getActivity());
                        }
                    }
                }

                break;
        }
    }

    @Override
    public void onConnected(DataService service) {
        Log.d(TAG, "onConnected");
        this.service = service;

        service.getMovieController().loadMovieCollection(this);
        loadTimers(service);
    }

    public MovieCollectionAdapter createAdapter() {
        if(loadingAdapter == null) {
            loadingAdapter = new MovieCollectionAdapter(getActivity(), service.getConnection());
            setupPreferences(loadingAdapter);
        }

        return loadingAdapter;
    }

    @Override
    public void onConnectionError(DataService service) {
        Log.d(TAG, "onConnectionError");

        // missing setup -> start setup activity
        if(TextUtils.isEmpty(SetupUtils.getServer(getActivity()))) {
            startSetupActivity();
            return;
        }

        MovieCollectionAdapter adapter = createAdapter();
        setupPreferences(adapter);
        setAdapter(adapter);

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
        MovieCollectionAdapter adapter = createAdapter();
        service.getTimerController().loadTimers(adapter::loadTimers);
    }
}
