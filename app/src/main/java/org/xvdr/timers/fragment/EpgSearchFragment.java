package org.xvdr.timers.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.ProgressBarManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.client.Channels;
import org.xvdr.timers.activity.EpgSearchActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.timers.presenter.EpgEventPresenter;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EpgSearchFragment extends SearchFragment implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 500;

    class DelayedTask implements Runnable {

        String query;

        void setSearchQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            if(mLoader != null) {
                mLoader.cancel(true);
            }

            mLoader = new EpgSearchLoader();
            mLoader.execute(query);
        }
    }

    class EpgSearchLoader extends AsyncTask<String, Void, List<ListRow>> {

        List<ListRow> mResultRows = new ArrayList<>();
        EpgEventPresenter eventPresenter = new EpgEventPresenter();

        private ListRow findOrCreateChannelRow(String channelName, long channelId) {
            // row already exists ?
            for(int i = 0; i < mResultRows.size(); i++) {
                ListRow row = mResultRows.get(i);

                if(row.getHeaderItem().getId() == channelId) {
                    return row;
                }
            }

            // add new row
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(eventPresenter);
            HeaderItem header = new HeaderItem(channelId, channelName);

            ListRow row = new ListRow(header, listRowAdapter);
            mResultRows.add(row);

            return row;
        }

        @Override
        protected void onPreExecute() {
            progress.show();
            progress.enableProgressBar();

            mRowsAdapter.clear();
        }

        @Override
        protected List<ListRow> doInBackground(String... params) {
            final Channels channelList = new Channels();
            channelList.load(mConnection);

            // search
            Packet req = mConnection.CreatePacket(
                             Connection.XVDR_EPG_SEARCH,
                             Connection.XVDR_CHANNEL_REQUEST_RESPONSE);

            req.putString(params[0]);

            Packet resp = mConnection.transmitMessage(req);

            if(resp == null || resp.eop()) {
                findOrCreateChannelRow(getString(R.string.no_search_results, params[0]), 0);
                return mResultRows;
            }

            // uncompress respsonse
            resp.uncompress();

            // process result
            int count = 0;
            while(!resp.eop() && !isCancelled()) {
                final Event event = ArtworkUtils.packetToEvent(resp);
                String channelName = resp.getString();
                int channelId = (int) resp.getU32();

                ListRow row = findOrCreateChannelRow(channelName, channelId);
                ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getAdapter();

                final Movie movie = new Movie();
                movie.setTitle(event.getTitle());
                movie.setOutline(event.getSubTitle());
                movie.setTimeStamp(event.getTimestamp().getTime());
                movie.setChannelName(channelName);
                movie.setContent(event.getContentId());
                movie.setChannelUid(channelId);
                movie.setStartTime(event.getStartTime());
                movie.setDuration(event.getDuration());

                // fetch artwork
                try {
                    ArtworkHolder holder = mArtwork.fetchForEvent(movie.getEvent());
                    movie.setArtwork(holder);
                }
                catch(IOException e) {
                    e.printStackTrace();
                }

                adapter.add(movie);
                count++;

                if(count >= 100) {
                    break;
                }
            }

            Collections.sort(mResultRows, new Comparator<ListRow>() {
                @Override
                public int compare(ListRow a, ListRow b) {
                    Channels.Entry entry1 = channelList.findByUid((int)a.getId());
                    Channels.Entry entry2 = channelList.findByUid((int)b.getId());
                    return  entry1.number < entry2.number ? -1 : 1;
                }
            });

            return mResultRows;
        }

        @Override
        protected void onPostExecute(final List<ListRow> result) {
            progress.disableProgressBar();
            progress.hide();

            for(ListRow row : result) {
                mRowsAdapter.add(row);
            }
        }
    }

    private ArrayObjectAdapter mRowsAdapter;
    private EpgSearchLoader mLoader;
    private DelayedTask mDelayedLoader;
    private Connection mConnection;
    private ArtworkFetcher mArtwork;
    private Handler mHandler;
    ProgressBarManager progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnection = new Connection("robotv:epgsearchhandler");
        mConnection.open(SetupUtils.getServer(getActivity()));

        mArtwork = new ArtworkFetcher(mConnection, SetupUtils.getLanguage(getActivity()));

        mHandler = new Handler();

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setTitle(getString(R.string.search_epg));

        setSearchResultProvider(this);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                Movie movie = (Movie) item;

                EpgSearchActivity activity = (EpgSearchActivity) getActivity();
                activity.selectEvent(movie);
            }
        });

        mLoader = new EpgSearchLoader();
        mDelayedLoader = new DelayedTask();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        progress = new ProgressBarManager();
        progress.setRootView((ViewGroup) view);

        return view;
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
        if(!TextUtils.isEmpty(newQuery)) {
            mRowsAdapter.clear();
            mDelayedLoader.setSearchQuery(newQuery);
            mHandler.removeCallbacks(mDelayedLoader);
            mHandler.postDelayed(mDelayedLoader, SEARCH_DELAY_MS);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!TextUtils.isEmpty(query)) {
            mRowsAdapter.clear();
            mDelayedLoader.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoader);
            mHandler.postDelayed(mDelayedLoader, SEARCH_DELAY_MS);
        }

        return true;
    }
}
