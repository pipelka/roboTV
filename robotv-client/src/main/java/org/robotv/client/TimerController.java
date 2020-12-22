package org.robotv.client;

import android.os.AsyncTask;
import android.util.Log;

import org.robotv.msgexchange.Packet;
import org.robotv.client.model.Movie;
import org.robotv.client.model.Timer;

import java.util.ArrayList;
import java.util.Collection;

public class TimerController {

    private static class TimerLoaderTask extends AsyncTask<Void, Void, ArrayList<Timer>> {

        final Connection connection;
        final LoaderCallback listener;

        TimerLoaderTask(Connection connection, LoaderCallback listener) {
            this.connection = connection;
            this.listener = listener;
        }

        @Override
        protected ArrayList<Timer> doInBackground(Void... params) {
            Packet request = connection.CreatePacket(Connection.TIMER_GETLIST);
            Packet response = connection.transmitMessage(request);

            if(response == null) {
                return null;
            }

            int count = (int) response.getU32();
            ArrayList<Timer> timers = new ArrayList<>(count);

            while(!response.eop()) {
                Timer timer = PacketAdapter.toTimer(response);
                timers.add(timer);
            }

            return timers;
        }

        @Override
        protected void onPostExecute(ArrayList<Timer> result) {
            if(listener != null) {
                listener.onTimersUpdated(result);
            }
        }

    }

    private static class SearchTimerLoaderTask extends AsyncTask<Void, Void, ArrayList<Timer>> {

        final Connection connection;
        final LoaderCallback listener;

        SearchTimerLoaderTask(Connection connection, LoaderCallback listener) {
            this.connection = connection;
            this.listener = listener;
        }
        @Override
        protected ArrayList<Timer> doInBackground(Void... params) {
            Packet request = connection.CreatePacket(Connection.SEARCHTIMER_GETLIST);
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

            ArrayList<Timer> timers = new ArrayList<>(10);

            while(!response.eop()) {
                Timer timer = PacketAdapter.toSearchTimer(response);
                timers.add(timer);
            }

            return timers;
        }

        @Override
        protected void onPostExecute(ArrayList<Timer> result) {
            if(listener != null) {
                listener.onTimersUpdated(result);
            }
        }

    }

    public interface LoaderCallback {
        void onTimersUpdated(ArrayList<Timer> timers);
    }

    private static final String TAG = "TimerController";

    final private Connection connection;

    private final int priority = 80;

    public TimerController(Connection connection) {
        this.connection = connection;
    }

    public boolean createTimer(Movie movie, String seriesFolder) {
        Timer timer = new Timer(0, movie);

        if (movie.isTvShow()) {
            timer.setFolder(seriesFolder);
        } else {
            timer.setFolder(movie.getFolder());
        }

        Packet request = connection.CreatePacket(Connection.TIMER_ADD);
        PacketAdapter.toPacket(timer, priority, request);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == 0;
    }

    public boolean updateTimer(Timer timer) {
        Packet request = connection.CreatePacket(Connection.TIMER_UPDATE);
        PacketAdapter.toPacket(timer, priority, request);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == 0;
    }

    public boolean createSearchTimer(Movie movie) {
        Packet request = connection.CreatePacket(Connection.SEARCHTIMER_ADD);
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
        TimerLoaderTask task = new TimerLoaderTask(connection, listener);
        task.execute();
    }

    public void loadSearchTimers(final LoaderCallback listener) {
        SearchTimerLoaderTask task = new SearchTimerLoaderTask(connection, listener);
        task.execute();
    }

    public boolean deleteTimer(int id) {
        Packet request = connection.CreatePacket(Connection.TIMER_DELETE);
        request.putU32(id);

        Packet response = connection.transmitMessage(request);

        return response != null && response.getU32() == Connection.STATUS_SUCCESS;
    }
}
