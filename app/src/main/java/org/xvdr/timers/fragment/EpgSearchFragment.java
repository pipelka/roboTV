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
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EpgSearchFragment extends SearchFragment implements SearchFragment.SearchResultProvider, DataService.Listener  {

    private static final int SEARCH_DELAY_MS = 500;

    class DelayedTask implements Runnable {

        String query;

        void setSearchQuery(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            if(loader != null) {
                loader.cancel(true);
            }

            loader = new EpgSearchLoader();
            loader.execute(query);
        }
    }

    class EpgSearchLoader extends AsyncTask<String, Void, List<ListRow>> {

        List<ListRow> resultRows = new ArrayList<>();
        EpgEventPresenter eventPresenter = new EpgEventPresenter();

        private ListRow findOrCreateChannelRow(String channelName, long channelId) {
            // row already exists ?
            for(int i = 0; i < resultRows.size(); i++) {
                ListRow row = resultRows.get(i);

                if(row.getHeaderItem().getId() == channelId) {
                    return row;
                }
            }

            // add new row
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(eventPresenter);
            HeaderItem header = new HeaderItem(channelId, channelName);

            ListRow row = new ListRow(header, listRowAdapter);
            resultRows.add(row);

            return row;
        }

        @Override
        protected void onPreExecute() {
            progress.show();
            progress.enableProgressBar();

            rowsAdapter.clear();
        }

        @Override
        protected List<ListRow> doInBackground(String... params) {
            final Channels channelList = new Channels();
            channelList.load(connection);

            // search
            Packet req = connection.CreatePacket(
                             Connection.XVDR_EPG_SEARCH,
                             Connection.XVDR_CHANNEL_REQUEST_RESPONSE);

            req.putString(params[0]);

            Packet resp = connection.transmitMessage(req);

            if(resp == null || resp.eop()) {
                findOrCreateChannelRow(getString(R.string.no_search_results, params[0]), 0);
                return resultRows;
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
                    ArtworkHolder holder = artwork.fetchForEvent(movie.getEvent());
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

            Collections.sort(resultRows, new Comparator<ListRow>() {
                @Override
                public int compare(ListRow a, ListRow b) {
                    Channels.Entry entry1 = channelList.findByUid((int)a.getId());
                    Channels.Entry entry2 = channelList.findByUid((int)b.getId());
                    return  entry1.number < entry2.number ? -1 : 1;
                }
            });

            return resultRows;
        }

        @Override
        protected void onPostExecute(final List<ListRow> result) {
            progress.disableProgressBar();
            progress.hide();

            for(ListRow row : result) {
                rowsAdapter.add(row);
            }

            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
        }
    }

    private ArrayObjectAdapter rowsAdapter;
    private EpgSearchLoader loader;
    private DelayedTask delayedLoader;
    private Connection connection;
    private ArtworkFetcher artwork;
    private Handler handler;
    ProgressBarManager progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
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

        loader = new EpgSearchLoader();
        delayedLoader = new DelayedTask();
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
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return rowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        if(!TextUtils.isEmpty(newQuery)) {
            rowsAdapter.clear();
            delayedLoader.setSearchQuery(newQuery);
            handler.removeCallbacks(delayedLoader);
            handler.postDelayed(delayedLoader, SEARCH_DELAY_MS);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!TextUtils.isEmpty(query)) {
            rowsAdapter.clear();
            delayedLoader.setSearchQuery(query);
            handler.removeCallbacks(delayedLoader);
            handler.postDelayed(delayedLoader, SEARCH_DELAY_MS);
        }

        return true;
    }

    @Override
    public void onConnected(DataService service) {
        connection = service.getConnection();
        artwork = new ArtworkFetcher(connection, SetupUtils.getLanguage(getActivity()));
    }

    @Override
    public void onMovieUpdate(DataService service) {
    }

}
