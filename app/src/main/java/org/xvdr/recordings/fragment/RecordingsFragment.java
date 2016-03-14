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
import android.widget.Toast;

import org.xvdr.recordings.activity.DetailsActivity;
import org.xvdr.recordings.activity.SearchActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.model.MovieCollectionLoader;
import org.xvdr.recordings.presenter.PreferenceCardPresenter;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;
import org.xvdr.robotv.client.Connection;

public class RecordingsFragment extends BrowseFragment {

    private final static String TAG = "RecordingsFragment";

    private Connection mConnection;
    private SpinnerFragment mSpinnerFragment;
    private MovieCollectionAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnection = new Connection("roboTV recordings");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initUI();
        loadMovies();
        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnection.close();
    }

    private void loadMovies() {
        new MovieCollectionLoader(mConnection, SetupUtils.getLanguage(getActivity())).load(new MovieCollectionLoader.Listener() {
            @Override
            public void onStart() {
                mSpinnerFragment = new SpinnerFragment();
                getFragmentManager().beginTransaction().add(R.id.main_browse_fragment, mSpinnerFragment).commit();

                if(!mConnection.open(SetupUtils.getServer(getActivity()))) {
                    Log.e(TAG, "unable to open connection");
                }

            }

            @Override
            public void onCompleted(MovieCollectionAdapter adapter) {
                mAdapter = adapter;

                setAdapter(adapter);
                setupPreferences(adapter);

                FragmentManager fragmentManager = getFragmentManager();

                if(fragmentManager != null) {
                    getFragmentManager().beginTransaction().remove(mSpinnerFragment).commit();
                }
            }
        });
    }

    private void setupPreferences(ArrayObjectAdapter adapter) {
        if(adapter == null) {
            return;
        }

        HeaderItem gridHeader = new HeaderItem(adapter.size(), "Preferences");
        PreferenceCardPresenter mGridPresenter = new PreferenceCardPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.setup_activity));
        adapter.add(new ListRow(gridHeader, gridRowAdapter));

    }

    private void setBackground() {
        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setColor(getResources().getColor(R.color.recordings_background, null));
    }

    private void initUI() {
        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);

        //Back button goes to the fast lane, rather than home screen
        setHeadersTransitionOnBackEnabled(true);

        setBrandColor(getResources().getColor(R.color.recordings_fastlane_background, null));
        setSearchAffordanceColor(getResources().getColor(R.color.recordings_search_button_color, null));
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
                        Intent intent = new Intent(getActivity(), DetailsActivity.class);
                        intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
                        startActivity(intent);
                    }
                }
                else if(item instanceof String) {
                    if(((String) item).equalsIgnoreCase(getString(R.string.setup_activity))) {
                        Intent intent = new Intent(getActivity(), org.xvdr.robotv.setup.SetupActivity.class);
                        startActivity(intent);
                    }
                }
            }
        };
    }

}
