package org.xvdr.recordings.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;

import org.xvdr.jniwrap.Packet;
import org.xvdr.recordings.activity.DetailsActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionAdapter;
import org.xvdr.recordings.model.PacketAdapter;
import org.xvdr.recordings.model.SortedArrayObjectAdapter;
import org.xvdr.recordings.presenter.MoviePresenter;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

public class MovieSearchFragment extends SearchFragment implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;

    private class SearchRunnable implements Runnable {

        private String query;

        @Override
        public void run() {
            ArrayObjectAdapter listRowAdapter = new SortedArrayObjectAdapter(MovieCollectionAdapter.compareTimestamps, new MoviePresenter());

            Packet req = mConnection.CreatePacket(Connection.XVDR_RECORDINGS_SEARCH, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
            req.putString(query);

            Packet resp = mConnection.transmitMessage(req);

            // no results
            if(resp == null || resp.eop()) {
                HeaderItem header = new HeaderItem(getString(R.string.no_search_results, query));
                ListRow listRow = new ListRow(header, new ArrayObjectAdapter());
                mRowsAdapter.add(listRow);
                return;
            }

            // results
            HeaderItem header = new HeaderItem(getString(R.string.search_results, query));
            ListRow listRow = new ListRow(header, listRowAdapter);

            while(!resp.eop()) {
                Movie movie = PacketAdapter.toMovie(resp);
                listRowAdapter.add(movie);
            }

            mRowsAdapter.add(listRow);
            mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
        }

        public void setSearchQuery(String newQuery) {
            query = newQuery;
        }
    }

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;
    private Connection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                Movie movie = (Movie) item;

                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(VideoDetailsFragment.EXTRA_MOVIE, movie);
                startActivity(intent);
            }
        });

        mDelayedLoad = new SearchRunnable();
        mConnection = new Connection("roboTV search movies");

        mConnection.open(SetupUtils.getServer(getActivity()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnection.close();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        mRowsAdapter.clear();

        if(!TextUtils.isEmpty(newQuery)) {
            mDelayedLoad.setSearchQuery(newQuery);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mRowsAdapter.clear();

        if(!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }
}
