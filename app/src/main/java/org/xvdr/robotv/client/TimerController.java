package org.xvdr.robotv.client;

import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.recordings.model.Movie;

public class TimerController {

    private static final String TAG = "TimerController";

    private Connection connection;
    private int mPreStartRecording = 2 * 60;
    private int mPostEndRecording = 5 * 60;
    private int mPriority = 80;

    public TimerController(Connection connection) {
        this.connection = connection;
    }

    public boolean createTimer(Movie movie, String seriesFolder) {
        String name;
        TimerController timer = new TimerController(connection);

        String category = movie.getFolder();

        if(category.equals(seriesFolder)) {
            name = seriesFolder + "~" + movie.getTitle() + "~" + movie.getShortText();
        }
        else {
            name = category;

            if (!name.isEmpty()) {
                name += "~";
            }

            name += movie.getTitle();
        }

        return timer.createTimer(movie.getChannelUid(), movie.getStartTime(), movie.getDuration(), name);
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
        request.putU32(mPriority); // Priority
        request.putU32(99); // Lifetime
        request.putU32(channelUid); // channel uid
        request.putU32(startTime - mPreStartRecording); // start time
        request.putU32(startTime + duration + mPostEndRecording); // end time
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

    private String mapRecordingName(String name) {
        return name.replace(' ', '_').replace(':', '_');
    }
}
