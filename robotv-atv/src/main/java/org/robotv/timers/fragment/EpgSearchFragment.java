package org.robotv.timers.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SearchOrbView;

import org.robotv.dataservice.DataService;
import org.robotv.msgexchange.Packet;
import org.robotv.client.PacketAdapter;
import org.robotv.recordings.util.Utils;
import org.robotv.timers.activity.EpgSearchActivity;
import org.robotv.client.model.Movie;
import org.robotv.timers.presenter.EpgEventPresenter;
import org.robotv.robotv.R;
import org.robotv.client.model.Event;
import org.robotv.client.Connection;
import org.robotv.ui.SearchProgressFragment;

import java.util.ArrayList;
import java.util.List;

public class EpgSearchFragment extends SearchProgressFragment implements DataService.Listener {

    private static final int SEARCH_DELAY_MS = 500;

    private class DelayedTask implements Runnable {

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

    private class EpgSearchLoader extends AsyncTask<String, Void, List<ListRow>> {

        List<ListRow> resultRows = new ArrayList<>();
        EpgEventPresenter eventPresenter = new EpgEventPresenter(connection);

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
            rowsAdapter.clear();
        }

        @Override
        protected List<ListRow> doInBackground(String... params) {

            // search
            Packet req = connection.CreatePacket(
                             Connection.EPG_SEARCH,
                             Connection.CHANNEL_REQUEST_RESPONSE);

            req.putString(params[0]);

            Packet resp = connection.transmitMessage(req);

            if(resp == null || resp.eop()) {
                findOrCreateChannelRow(getString(R.string.no_search_results, params[0]), 0);
                return resultRows;
            }

            // uncompress respsonse
            resp.uncompress();

            // process result
            while(!resp.eop() && !isCancelled()) {
                final Event event = PacketAdapter.toEvent(resp);
                String channelName = resp.getString();
                int channelUid = (int) resp.getU32();
                int channelNumber = (int) resp.getU32();

                ListRow row = findOrCreateChannelRow(channelName, channelNumber);
                ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getAdapter();

                final Movie movie = new Movie(event);
                movie.setChannelName(channelName);
                movie.setChannelUid(channelUid);

                adapter.add(movie);
            }

            resultRows.sort((a, b) -> a.getId() < b.getId() ? -1 : 1);

            return resultRows;
        }

        @Override
        protected void onPostExecute(final List<ListRow> result) {
            for(ListRow row : result) {
                rowsAdapter.add(row);
            }

            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
            showProgress(false);
        }
    }

    private ArrayObjectAdapter rowsAdapter;
    private EpgSearchLoader loader;
    private DelayedTask delayedLoader;
    private Connection connection;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setTitle(getString(R.string.search_epg));

        setSearchAffordanceColorsInListening(new SearchOrbView.Colors(
                Utils.getColor(getContext(), R.color.recordings_search_button_color),
                Utils.getColor(getContext(), R.color.primary_color_light),
                Utils.getColor(getContext(), R.color.primary_color)
        ));

        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            Movie movie = (Movie) item;

            EpgSearchActivity activity = (EpgSearchActivity) getActivity();
            activity.selectEvent(movie);
        });

        loader = new EpgSearchLoader();
        delayedLoader = new DelayedTask();
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
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!super.onQueryTextSubmit(query)) {
            return false;
        }

        rowsAdapter.clear();
        delayedLoader.setSearchQuery(query);
        handler.removeCallbacks(delayedLoader);
        handler.postDelayed(delayedLoader, SEARCH_DELAY_MS);

        return true;
    }

    @Override
    public void onConnected(DataService service) {
        setSearchResultProvider(this);
        connection = service.getConnection();
    }

    @Override
    public void onConnectionError(DataService service) {

    }

    @Override
    public void onMovieUpdate(DataService service) {

    }

    @Override
    public void onTimersUpdated(DataService service) {

    }

}
