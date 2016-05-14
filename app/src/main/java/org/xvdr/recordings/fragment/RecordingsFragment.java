package org.xvdr.recordings.fragment;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;

import android.support.v17.leanback.widget.Row;
import android.util.Log;
import android.view.View;

import org.xvdr.recordings.activity.DetailsActivity;
import org.xvdr.recordings.activity.SearchActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.model.MovieCollectionLoader;
import org.xvdr.recordings.presenter.PreferenceCardPresenter;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.Connection;

public class RecordingsFragment extends BrowseFragment {

    private final static String TAG = "RecordingsFragment";

    private SpinnerFragment mSpinnerFragment;
    private MovieCollectionAdapter mAdapter;
    private ArrayObjectAdapter mRowAdapter = null;
    private String mLastServer = "";

    private int color_background;
    private int color_brand;

    private MovieCollectionLoader.Listener mListener = new MovieCollectionLoader.Listener() {
        @Override
        public void onStart() {
            mSpinnerFragment = new SpinnerFragment();
            getFragmentManager().beginTransaction().add(R.id.container, mSpinnerFragment).commit();
        }

        @Override
        public void onCompleted(MovieCollectionAdapter adapter) {
            FragmentManager fragmentManager = getFragmentManager();

            if(fragmentManager != null) {
                getFragmentManager().beginTransaction().remove(mSpinnerFragment).commit();
            }

            if(mAdapter == null) {
                mAdapter = adapter;
                setAdapter(adapter);
                setupPreferences(adapter);
            }

            if(mRowAdapter != null) {
                mRowAdapter.notifyArrayItemRangeChanged(0, mRowAdapter.size());
                mRowAdapter = null;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initUI();
        loadMovies(false);
        setupEventListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMovies(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loadMovies(boolean onlyUpdate) {
        Connection connection = new Connection("roboTV recordings");

        String currentServer = SetupUtils.getServer(getActivity());

        if(!mLastServer.equals(currentServer)) {
            onlyUpdate = false;
            mAdapter = null;
        }

        if(!connection.open(currentServer)) {
            Log.e(TAG, "unable to open connection");
        }

        mLastServer = SetupUtils.getServer(getActivity());

        if(onlyUpdate) {
            if(mAdapter == null) {
                connection.close();
                return;
            }

            new MovieCollectionLoader(connection, SetupUtils.getLanguage(getActivity()), mAdapter).load(mListener);
        }
        else {
            new MovieCollectionLoader(getActivity(), connection, SetupUtils.getLanguage(getActivity())).load(mListener);
        }
    }

    private void setupPreferences(ArrayObjectAdapter adapter) {
        if(adapter == null) {
            return;
        }

        HeaderItem gridHeader = new HeaderItem(
            adapter.size(),
            getActivity().getString(R.string.recordings_settings_title));
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new PreferenceCardPresenter());

        gridRowAdapter.add(new PreferenceCardPresenter.Style(
                               1,
                               getActivity().getString(R.string.recordings_setup_title),
                               R.drawable.ic_settings_white_48dp));

        adapter.add(new ListRow(gridHeader, gridRowAdapter));

    }

    private void setBackground() {
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setColor(color_background);
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

                    if(movie.isSeriesHeader()) {
                        mAdapter.setSeriesRow(movie.getTitle());
                    }
                    else {
                        mRowAdapter = mAdapter.getCategory(movie);
                        Intent intent = new Intent(getActivity(), DetailsActivity.class);
                        intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
                        startActivity(intent);
                    }
                }
                else if(item instanceof PreferenceCardPresenter.Style) {
                    if(((PreferenceCardPresenter.Style) item).getId() == 1) {
                        Intent intent = new Intent(getActivity(), org.xvdr.robotv.setup.SetupActivity.class);
                        startActivity(intent);
                    }
                }
            }
        };
    }

}
