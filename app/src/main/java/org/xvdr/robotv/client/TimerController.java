package org.xvdr.robotv.client;

import android.os.AsyncTask;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
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

    private int preStartRecording = 2 * 60;
    private int postEndRecording = 5 * 60;
    private int priority = 80;

    public TimerController(Connection connection, String language) {
        this.connection = connection;
        this.language = language;
    }

    public boolean createTimer(Movie movie, String seriesFolder) {
        String name;
        String category = movie.getFolder();

        if(category.equals(seriesFolder) || movie.isTvShow()) {
            name = seriesFolder + "~" + movie.getTitle() + "~" + movie.getShortText();
        }
        else {
            name = category;

            if (!name.isEmpty()) {
                name += "~";
            }

            name += movie.getTitle();
        }

        return createTimer(movie.getChannelUid(), movie.getStartTime(), movie.getDuration(), name);
    }

    public boolean createTimer(int channelUid, long startTime, int duration, String name) {
        Log.d(TAG, "CREATE TIMER");
        Log.d(TAG, "channelUid: " + channelUid);
        Log.d(TAG, "startTime: " + startTime);
        Log.d(TAG, "duration: " + duration);
        Log.d(TAG, "name: " + name);

        Packet request = connection.CreatePacket(Connection.ROBOTV_TIMER_ADD);
        request.putU32(0); // index unused
        request.putU32(1 + 4); // active timer + VPS
        request.putU32(priority); // Priority
        request.putU32(99); // Lifetime
        request.putU32(channelUid); // channel uid
        request.putU32(startTime - preStartRecording); // start time
        request.putU32(startTime + duration + postEndRecording); // end time
        request.putU32(0); // day
        request.putU32(0); // weeksdays
        request.putString(mapRecordingName(name)); // recording name
        request.putString(""); // aux

        Packet response = connection.transmitMessage(request);

        if(response == null) {
            return false;
        }

        return response.getU32() == 0;
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
                        o = fetcher.fetchForEvent(timer);
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(o != null) {
                        timer.setPosterUrl(o.getBackgroundUrl());
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
                        o = fetcher.fetchForEvent(timer);
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(o != null) {
                        timer.setPosterUrl(o.getBackgroundUrl());
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

    private String mapRecordingName(String name) {
        return name.replace(' ', '_').replace(':', '_');
    }
}
