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
import android.support.v17.leanback.widget.ListRowPresenter;
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
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.presenter.PreferenceCardPresenter;
import org.xvdr.recordings.util.BackgroundManagerTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

import java.util.Collection;

public class RecordingsFragment extends BrowseFragment implements DataServiceClient.Listener {

    private final static String TAG = "RecordingsFragment";

    private MovieCollectionAdapter mAdapter;
    private BackgroundManager backgroundManager;
    private BackgroundManagerTarget backgroundManagerTarget;

    private int color_background;
    private int color_brand;
    private int selectedRow = -1;
    private int selectedItem = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupEventListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        prepareEntranceTransition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    synchronized private void loadMovies(Collection<Movie> collection) {
        Log.d(TAG, "loadMovies");

        mAdapter = new MovieCollectionAdapter(getActivity());

        if(collection.size() != 0) {
            mAdapter.addAllMovies(collection);
        }

        setupPreferences(mAdapter);
        mAdapter.cleanup();

        setAdapter(mAdapter);

        // update current row
        if(selectedRow >= 0 && selectedRow < mAdapter.size()) {
            if(selectedItem >= 0) {
                try {
                    ListRowPresenter.SelectItemViewHolderTask task = new ListRowPresenter.SelectItemViewHolderTask(selectedItem);
                    task.setSmoothScroll(false);

                    setSelectedPosition(selectedRow, false, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                setSelectedPosition(selectedRow);
            }
        }

        startEntranceTransition();
    }

    @Override
    public OnItemViewSelectedListener getOnItemViewSelectedListener() {
        return new OnItemViewSelectedListener() {

            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                selectedRow = getSelectedPosition();
                ListRow listRow = (ListRow) row;
                ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter)listRow.getAdapter();

                if(item instanceof Movie) {
                    selectedItem = rowAdapter.indexOf(item);
                    Movie movie = (Movie) item;
                    updateBackground(movie.getBackgroundImageUrl());
                }
                else {
                    selectedItem = -1;
                }
            }
        };
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

    private void setupPreferences(ArrayObjectAdapter adapter) {
        if(adapter == null) {
            return;
        }

        HeaderItem gridHeader = new HeaderItem(
            1000,
            getActivity().getString(R.string.recordings_settings_title));
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new PreferenceCardPresenter());

        gridRowAdapter.add(new PreferenceCardPresenter.Style(
                               1,
                               getActivity().getString(R.string.recordings_setup_title),
                               R.drawable.ic_settings_white_48dp));

        adapter.add(new ListRow(gridHeader, gridRowAdapter));

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
        setHeadersState(HEADERS_ENABLED);

        //Back button goes to the fast lane, rather than home screen
        setHeadersTransitionOnBackEnabled(true);

        color_background = Utils.getColor(getActivity(), R.color.recordings_background);
        color_brand = Utils.getColor(getActivity(), R.color.primary_color);

        setBrandColor(color_brand);
        setSearchAffordanceColor(Utils.getColor(getActivity(), R.color.recordings_search_button_color));
        setBackground();
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(getDefaultItemClickedListener());
        setOnItemViewSelectedListener(getOnItemViewSelectedListener());

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    protected OnItemViewClickedListener getDefaultItemClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                if(item instanceof Movie) {
                    Movie movie = (Movie) item;

                    selectedRow = getSelectedPosition();
                    ListRow listRow = (ListRow) row;
                    ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter)listRow.getAdapter();
                    selectedItem = rowAdapter.indexOf(item);

                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
                    startActivity(intent);
                }
                else if(item instanceof PreferenceCardPresenter.Style) {
                    if(((PreferenceCardPresenter.Style) item).getId() == 1) {
                        startSetupActivity();
                    }
                }
            }
        };
    }

    private void startSetupActivity() {
        Intent intent = new Intent(getActivity(), org.xvdr.robotv.setup.SetupActivity.class);
        startActivity(intent);
    }

    @Override
    public void onServiceConnected(DataService service) {
        if(service.getConnectionStatus() == DataService.STATUS_Server_Failed) {
            startSetupActivity();
        }

        service.notifyClientConnected();
    }

    @Override
    public void onServiceDisconnected(DataService service) {
    }

    @Override
    public void onMovieCollectionUpdated(DataService service, Collection<Movie> collection, int status) {
        if(getActivity() == null) {
            return;
        }

        Log.d(TAG, "onMovieCollectionUpdated status=" + status);

        ProgressBarManager manager = getProgressBarManager();

        if(status == DataService.STATUS_Collection_Busy) {
            manager.enableProgressBar();
            manager.show();
        }
        else if(status == DataService.STATUS_Collection_Ready) {
            manager.disableProgressBar();
            manager.hide();
        }

        if(collection != null) {
            loadMovies(collection);
        }
    }
}
