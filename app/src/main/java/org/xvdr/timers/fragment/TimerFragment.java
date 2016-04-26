package org.xvdr.timers.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;

import org.xvdr.msgexchange.Packet;
import org.xvdr.timers.activity.EpgSearchActivity;
import org.xvdr.recordings.model.Movie;
import org.xvdr.timers.presenter.EpgEventPresenter;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.IOException;

public class TimerFragment extends BrowseFragment {

    private final static String TAG = "TimerFragment";

    class EpgSearchLoader extends AsyncTask<Integer, Void, ArrayObjectAdapter> {

        @Override
        protected ArrayObjectAdapter doInBackground(Integer... params) {
            ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

            mConnection.open(SetupUtils.getServer(getActivity()));

            Packet req = mConnection.CreatePacket(
                             Connection.XVDR_EPG_GETFORCHANNEL,
                             Connection.XVDR_CHANNEL_REQUEST_RESPONSE);


            req.putU32(params[0]);
            req.putU32(System.currentTimeMillis() / 1000);
            req.putU32(60 * 60 * 24);

            Packet resp = mConnection.transmitMessage(req);

            // add new row
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new EpgEventPresenter());
            HeaderItem header = new HeaderItem(params[0], mChannelName);

            ListRow row = new ListRow(header, listRowAdapter);
            ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) row.getAdapter();

            rowsAdapter.add(row);

            // process result
            while(!resp.eop() && !isCancelled()) {
                int eventId = (int) resp.getU32();
                long startTime = resp.getU32();
                long endTime = startTime + resp.getU32();
                int content = (int) resp.getU32();
                int eventDuration = (int)(endTime - startTime);
                long parentalRating = resp.getU32();
                String title = resp.getString();
                String plotOutline = resp.getString();
                String plot = resp.getString();
                String posterUrl = resp.getString();
                String backgroundUrl = resp.getString();

                Event event = new Event(content, title, plotOutline, plot, eventDuration, eventId);
                ArtworkHolder art = null;

                try {
                    art = mArtwork.fetchForEvent(event);
                }
                catch(IOException e) {
                    e.printStackTrace();
                }

                final Movie movie = new Movie();
                movie.setTitle(event.getTitle());
                movie.setOutline(event.getSubTitle());
                movie.setTimeStamp(event.getTimestamp().getTime());
                movie.setChannelName(mChannelName);
                movie.setArtwork(art);
                movie.setTimeStamp(startTime * 1000);
                movie.setChannelUid(params[0]);
                movie.setDuration(eventDuration);

                rowAdapter.add(movie);
            }

            mConnection.close();
            return rowsAdapter;
        }

        @Override
        protected void onPostExecute(final ArrayObjectAdapter result) {
            TimerFragment.this.setAdapter(result);
        }
    }

    private Connection mConnection;
    private ArtworkFetcher mArtwork;
    private String mChannelName;
    private EpgSearchLoader mLoader;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String language = SetupUtils.getLanguage(getActivity());
        mConnection = new Connection("roboTV:addtimer", language);
        mArtwork = new ArtworkFetcher(mConnection, language);

        int channelUid = getActivity().getIntent().getIntExtra("uid", 0);
        mChannelName = getActivity().getIntent().getStringExtra("name");

        setTitle(getString(R.string.timer_title));
        setHeadersState(HEADERS_DISABLED);

        setHeadersTransitionOnBackEnabled(true);

        int color_background = Utils.getColor(getActivity(), R.color.recordings_background);
        int color_brand = Utils.getColor(getActivity(), R.color.primary_color);

        setBrandColor(color_brand);
        setSearchAffordanceColor(Utils.getColor(getActivity(), R.color.recordings_search_button_color));

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        backgroundManager.setColor(color_background);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
                Movie movie = (Movie) item;
                CreateTimerFragment.startGuidedStep(getActivity(), movie);
            }
        });

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), EpgSearchActivity.class);
                startActivity(intent);
            }
        });

        mLoader = new EpgSearchLoader();
        loadEpgForChannel(channelUid, mChannelName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLoader.cancel(true);
    }

    private void loadEpgForChannel(int uid, String name) {
        mChannelName = name;
        mLoader.execute(uid);
    }
}
