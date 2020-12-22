package org.robotv.recordings.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;

import org.robotv.recordings.activity.CoverSearchActivity;
import org.robotv.recordings.presenter.ArtworkPresenter;
import org.robotv.recordings.util.BackgroundManagerTarget;
import org.robotv.robotv.R;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.provider.TheMovieDatabase;
import org.robotv.client.artwork.provider.TheTvDb;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.SearchProgressFragment;

import java.util.List;

public class CoverSearchFragment extends SearchProgressFragment {

    private static final int SEARCH_DELAY_MS = 2000;
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

            getActivity().runOnUiThread(() -> {
                String title = list.isEmpty() ?
                        getString(R.string.no_search_results, query) :
                        getString(R.string.search_results, query);

                HeaderItem header = new HeaderItem(title);
                ListRow listRow = new ListRow(header, listRowAdapter);

                mRowsAdapter.add(listRow);
                listRowAdapter.addAll(0, list);

                showProgress(false);
            });
        }

        void setSearchQuery(String newQuery) {
            query = newQuery;
        }
    }

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler;
    private SearchRunnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());

        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setAutoReleaseOnStop(false);

        HandlerThread mHandlerThread = new HandlerThread("robotv:coversearchhandler", android.os.Process.THREAD_PRIORITY_DEFAULT);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            ArtworkHolder holder = (ArtworkHolder) item;

            CoverSearchActivity activity = (CoverSearchActivity) getActivity();
            activity.setArtwork(holder);
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
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!super.onQueryTextSubmit(query)) {
            return false;
        }

        mDelayedLoad.setSearchQuery(query);
        mHandler.removeCallbacks(mDelayedLoad);
        mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);

        return true;
    }
}
