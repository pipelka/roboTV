package org.robotv.timers.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.app.ProgressBarManager;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.robotv.msgexchange.Packet;
import org.robotv.client.PacketAdapter;
import org.robotv.dataservice.DataService;
import org.robotv.timers.activity.EpgSearchActivity;
import org.robotv.client.model.Movie;
import org.robotv.timers.activity.TimerActivity;
import org.robotv.timers.presenter.EpgEventPresenter;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.Connection;

public class TimerFragment extends DetailsSupportFragment implements DataService.Listener {

    private class EpgSearchLoader extends AsyncTask<Void, Void, ArrayObjectAdapter> {

        @Override
        protected ArrayObjectAdapter doInBackground(Void... params) {
            ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

            if(connection == null) {
                return rowsAdapter;
            }

            Packet req = connection.CreatePacket(
                             Connection.XVDR_EPG_GETFORCHANNEL,
                             Connection.XVDR_CHANNEL_REQUEST_RESPONSE);


            req.putU32(channelUid);
            req.putU32(System.currentTimeMillis() / 1000);
            req.putU32(60 * 60 * 24 * 2);

            Packet resp = connection.transmitMessage(req);

            // check if we got a response
            if(resp == null) {
                return rowsAdapter;
            }

            // uncompress response
            resp.uncompress();

            // add new row
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new EpgEventPresenter(connection));
            HeaderItem header = new HeaderItem(channelUid, channelName);

            ListRow row = new ListRow(header, listRowAdapter);
            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) row.getAdapter();

            rowsAdapter.add(row);

            // process result
            while(!resp.eop() && !isCancelled()) {
                final Movie movie = new Movie(PacketAdapter.toEpgEvent(resp));

                movie.setChannelUid(channelUid);
                movie.setChannelName(channelName);

                rowAdapter.add(movie);
            }

            return rowsAdapter;
        }

        @Override
        protected void onPreExecute() {
            progress.show();
            progress.enableProgressBar();
        }

        @Override
        protected void onPostExecute(final ArrayObjectAdapter result) {
            progress.disableProgressBar();
            progress.hide();

            TimerFragment.this.setAdapter(result);
            startEntranceTransition();
        }

        @Override
        protected void onCancelled(ArrayObjectAdapter result) {
            progress.disableProgressBar();
            progress.hide();

            TimerFragment.this.setAdapter(result);
            startEntranceTransition();
        }
    }

    private Connection connection;
    private String channelName;
    private int channelUid;
    private EpgSearchLoader loader;
    private ProgressBarManager progress;
    private Handler handler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        prepareEntranceTransition();

        channelUid = getActivity().getIntent().getIntExtra("uid", 0);
        channelName = getActivity().getIntent().getStringExtra("name");

        setTitle(getString(R.string.timer_title));

        setSearchAffordanceColor(Utils.getColor(getActivity(), R.color.recordings_search_button_color));

        handler = new Handler();

        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            Movie movie = (Movie) item;
            ((TimerActivity) getActivity()).selectEvent(movie);
        });

        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), EpgSearchActivity.class);
            startActivity(intent);
        });
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
        loader = null;
    }

    @Override
    public void onConnected(DataService service) {
        connection = service.getConnection();

        handler.post(() -> {
            //setHeadersState(HEADERS_HIDDEN);
            loadEpgForChannel();
        });
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

    private void loadEpgForChannel() {
        loader = new EpgSearchLoader();
        loader.execute();
    }
}
