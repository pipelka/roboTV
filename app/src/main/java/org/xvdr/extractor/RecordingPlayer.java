package org.xvdr.extractor;

import android.content.Context;
import android.util.Log;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.Connection;

import java.math.BigInteger;

public class RecordingPlayer extends Player {

    static final private String TAG = "RecordingPlayer";

    private int mDurationInMs = 0;
    private long mLengthInBytes = 0;
    private String mRecordingId;

    public RecordingPlayer(Context context, String server, String language, Listener listener, boolean audioPassthrough, int wantedChannelConfiguration) {
        super(context, server, language, listener, audioPassthrough, wantedChannelConfiguration, 200);
    }

    public int openRecording(String recordingId, boolean startAtLastPosition) {
        Log.d(TAG, "open recording: " + recordingId);
        mRecordingId = recordingId;

        close();
        open();

        Packet req = mConnection.CreatePacket(Connection.XVDR_RECSTREAM_OPEN, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(mRecordingId);

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            Log.e(TAG, "no response opening recording: " + recordingId);
            return Connection.STATUS_NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status > 0) {
            Log.e(TAG, "error opening recording, status: " + status);
            return status;
        }

        resp.getU32(); // 0
        mLengthInBytes = resp.getU64().longValue(); // length in bytes
        resp.getU8(); // TS / PES
        mDurationInMs = (int) resp.getU32() * 1000; // length in milliseconds

        Log.d(TAG, "length: " + mLengthInBytes + " bytes");
        Log.d(TAG, "duration: " + mDurationInMs / 1000 + " seconds");

        if(startAtLastPosition) {
            long startPosition = getLastPosition(recordingId);

            if(startPosition > 0) {
                startPosition += getStartPositionWallclock();
                setStartPosition(startPosition);
            }
        }

        prepare();

        return Connection.STATUS_SUCCESS;
    }

    public long getLastPosition(String id) {
        Packet req = mConnection.CreatePacket(Connection.XVDR_RECORDINGS_GETPOSITION, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(id);

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            return 0;
        }

        long position = resp.getU64().longValue();

        Log.d(TAG, "last position: " + position);
        return position;
    }

    public void setLastPosition(long position) {
        Packet req = mConnection.CreatePacket(Connection.XVDR_RECORDINGS_SETPOSITION, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(mRecordingId);
        req.putU64(BigInteger.valueOf(position));

        Log.d(TAG, "set last position: " + position);

        mConnection.transmitMessage(req);
    }

    public void setStartPosition(long position) {
        Packet req = mConnection.CreatePacket(Connection.XVDR_CHANNELSTREAM_SEEK, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putS64(position);

        mConnection.transmitMessage(req);
    }

    public int getDurationMs() {
        return mDurationInMs;
    }


}
