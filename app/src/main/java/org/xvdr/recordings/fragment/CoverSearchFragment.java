package org.xvdr.recordings.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
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
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.provider.TheMovieDatabase;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.List;

public class CoverSearchFragment extends SearchFragment implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;

    private class SearchRunnable implements Runnable {

        private String query;
        private TheMovieDatabase mMovieDb = new TheMovieDatabase(ArtworkFetcher.TMDB_APIKEY, SetupUtils.getLanguage(getActivity()));

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<ArtworkHolder> list = mMovieDb.searchAll(query);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListRowAdapter.clear();

                            for(ArtworkHolder item : list) {
                                mListRowAdapter.add(item);
                            }

                            mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
                        }
                    });
                }
            }).start();
        }

        public void setSearchQuery(String newQuery) {
            query = newQuery;
        }
    }

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;
    private ArrayObjectAdapter mListRowAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mDelayedLoad = new SearchRunnable();
        mListRowAdapter = new ArrayObjectAdapter(new ArtworkPresenter());

        ListRow listRow = new ListRow(null, mListRowAdapter);
        mRowsAdapter.add(listRow);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        mListRowAdapter.clear();

        if(!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }

        return true;
    }
}
