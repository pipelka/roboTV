package org.xvdr.robotv.client;

import android.os.AsyncTask;
import android.util.Log;

import org.robotv.msgexchange.Packet;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.model.Timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class TimerController {

    public interface LoaderCallback {
        void onTimersUpdated(Collection<Timer> timers);
    }

    private static final String TAG = "TimerController";

    final private Connection connection;
    final private String language;

    private int priority = 80;

    public TimerController(Connection connection, String language) {
        this.connection = connection;
        this.language = language;
    }

    public boolean createTimer(Movie movie, String seriesFolder) {
        Timer timer = new Timer(0, movie);

        if (movie.isTvShow()) {
            timer.setFolder(seriesFolder);
        } else {
            timer.setFolder(movie.getFolder());
        }

        Packet request = connection.CreatePacket(Connection.ROBOTV_TIMER_ADD);
        PacketAdapter.toPacket(timer, priority, request);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == 0;
    }

    public boolean updateTimer(Timer timer) {
        Packet request = connection.CreatePacket(Connection.ROBOTV_TIMER_UPDATE);
        PacketAdapter.toPacket(timer, priority, request);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == 0;
    }

    public boolean createSearchTimer(Movie movie) {
        Packet request = connection.CreatePacket(Connection.ROBOTV_SEARCHTIMER_ADD);
        request.putU32(movie.getChannelUid());
        request.putString(movie.getTitle());

        Packet response = connection.transmitMessage(request);

        if(response == null) {
            return false;
        }

        long status = response.getU32();

        Log.d(TAG, "createSearchTimer: status = " + status);

        return (status == 0);
    }

    public void loadTimers(final LoaderCallback listener) {
        AsyncTask<Void, Void, Collection<Timer>> task = new AsyncTask<Void, Void, Collection<Timer>>() {

            @Override
            protected Collection<Timer> doInBackground(Void... params) {
                ArtworkFetcher fetcher = new ArtworkFetcher(connection, language);
                Packet request = connection.CreatePacket(Connection.ROBOTV_TIMER_GETLIST);
                Packet response = connection.transmitMessage(request);

                if(response == null) {
                    return null;
                }

                int count = (int) response.getU32();
                Collection<Timer> timers = new ArrayList<>(count);

                while(!response.eop()) {
                    Timer timer = PacketAdapter.toTimer(response);

                    ArtworkHolder o = null;

                    try {
                        if(fetcher.fetchForEvent(timer)) {
                            timer.setPosterUrl(timer.getBackgroundUrl());
                        }
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    timers.add(timer);
                }

                return timers;
            }

            @Override
            protected void onPostExecute(Collection<Timer> result) {
                if(listener != null) {
                    listener.onTimersUpdated(result);
                }
            }

        };

        task.execute();
    }

    public void loadSearchTimers(final LoaderCallback listener) {
        AsyncTask<Void, Void, Collection<Timer>> task = new AsyncTask<Void, Void, Collection<Timer>>() {

            @Override
            protected Collection<Timer> doInBackground(Void... params) {
                ArtworkFetcher fetcher = new ArtworkFetcher(connection, language);
                Packet request = connection.CreatePacket(Connection.ROBOTV_SEARCHTIMER_GETLIST);
                Packet response = connection.transmitMessage(request);

                if(response == null) {
                    return null;
                }

                response.uncompress();

                int status = (int) response.getU32();

                if(status != 0) {
                    Log.e(TAG, "error loading search timers. status: " + status);
                    return null;
                }

                Collection<Timer> timers = new ArrayList<>(10);

                while(!response.eop()) {
                    Timer timer = PacketAdapter.toSearchTimer(response);

                    ArtworkHolder o = null;

                    try {
                        fetcher.fetchForEvent(timer);
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    timer.setPosterUrl(timer.getBackgroundUrl());
                    timers.add(timer);
                }

                return timers;
            }

            @Override
            protected void onPostExecute(Collection<Timer> result) {
                if(listener != null) {
                    listener.onTimersUpdated(result);
                }
            }

        };

        task.execute();
    }

    public boolean deleteTimer(int id) {
        Packet request = connection.CreatePacket(Connection.ROBOTV_TIMER_DELETE);
        request.putU32(id);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == Connection.STATUS_SUCCESS;
    }
}
