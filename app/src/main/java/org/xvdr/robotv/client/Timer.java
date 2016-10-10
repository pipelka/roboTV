package org.xvdr.robotv.client;

import android.util.Log;

import org.xvdr.msgexchange.Packet;

public class Timer {

    private static final String TAG = "Timer";

    private Connection mConnection;
    private int mPreStartRecording = 2 * 60;
    private int mPostEndRecording = 5 * 60;
    private int mPriority = 80;

    public Timer(Connection connection) {
        mConnection = connection;
    }

    public boolean create(int channelUid, long startTime, int duration, String name) {
        Log.d(TAG, "CREATE TIMER");
        Log.d(TAG, "channelUid: " + channelUid);
        Log.d(TAG, "startTime: " + startTime);
        Log.d(TAG, "duration: " + duration);
        Log.d(TAG, "name: " + name);

        Packet request = mConnection.CreatePacket(Connection.ROBOTV_TIMER_ADD);
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

        Packet response = mConnection.transmitMessage(request);

        if(response == null) {
            return false;
        }

        return response.getU32() == 0;
    }

    private String mapRecordingName(String name) {
        return name.replace(' ', '_').replace(':', '_');
    }
}
