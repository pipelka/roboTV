package org.xvdr.recordings.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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

import org.xvdr.recordings.activity.CoverSearchActivity;
import org.xvdr.recordings.presenter.ArtworkPresenter;
import org.xvdr.robotv.R;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.provider.TheMovieDatabase;
import org.robotv.client.artwork.provider.TheTvDb;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.List;

public class CoverSearchFragment extends SearchFragment implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;
    private TheMovieDatabase mMovieDb;
    private TheTvDb tvDb;

    private class SearchRunnable implements Runnable {

        private String query;
        @Override
        public void run() {
            mRowsAdapter.clear();

            final ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new ArtworkPresenter());
            final List<ArtworkHolder> list = mMovieDb.searchAll(query);

            list.addAll(tvDb.searchAll(query));

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String title = list.isEmpty() ?
                            getString(R.string.no_search_results, query) :
                            getString(R.string.search_results, query);

                    HeaderItem header = new HeaderItem(title);
                    ListRow listRow = new ListRow(header, listRowAdapter);
                    mRowsAdapter.add(listRow);

                    listRowAdapter.addAll(0, list);
                }
            });
        }

        void setSearchQuery(String newQuery) {
            query = newQuery;
        }
    }

    private ArrayObjectAdapter mRowsAdapter;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private SearchRunnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandlerThread = new HandlerThread("robotv:coversearchhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                ArtworkHolder holder = (ArtworkHolder) item;

                CoverSearchActivity activity = (CoverSearchActivity) getActivity();
                activity.setArtwork(holder);
            }
        });

        mMovieDb = new TheMovieDatabase(ArtworkFetcher.TMDB_APIKEY, SetupUtils.getLanguage(getActivity()));
        tvDb = new TheTvDb(SetupUtils.getLanguage(getActivity()));

        mDelayedLoad = new SearchRunnable();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        if(!TextUtils.isEmpty(newQuery)) {
            mDelayedLoad.setSearchQuery(newQuery);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }
}
