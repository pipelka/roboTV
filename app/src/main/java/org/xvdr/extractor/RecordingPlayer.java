package org.xvdr.extractor;

import android.content.Context;
import android.util.Log;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.Connection;

public class RecordingPlayer extends Player {

    static final private String TAG = "RecordingPlayer";

    private int mDurationInMs = 0;
    private long mLengthInBytes = 0;

    public RecordingPlayer(Context context, String server, String language, Listener listener) {
        super(context, server, language, listener);
    }

    public int openRecording(String recordingId) {
        Log.d(TAG, "open recording: " + recordingId);

        close();
        open();

        Packet req = mConnection.CreatePacket(Connection.XVDR_RECSTREAM_OPEN, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(recordingId);

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            Log.e(TAG, "no response opening recording: " + recordingId);
            return Connection.NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status > 0) {
            Log.e(TAG, "error opening recording, status: " + status);
            return status;
        }

        prepare();

        resp.getU32(); // 0
        mLengthInBytes = resp.getU64().longValue(); // length in bytes
        resp.getU8(); // TS / PES
        mDurationInMs = (int) resp.getU32() * 1000; // length in milliseconds

        Log.d(TAG, "length: " + mLengthInBytes + " bytes");
        Log.d(TAG, "duration: " + mDurationInMs / 1000 + " seconds");

        return Connection.SUCCESS;
    }

}
