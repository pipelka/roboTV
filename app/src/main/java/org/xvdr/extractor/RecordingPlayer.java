package org.xvdr.extractor;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.SeekMap;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tv.ServerConnection;

import java.math.BigInteger;

public class RecordingPlayer extends Player {

    static final private String TAG = "RecordingPlayer";

    protected class LoaderThread extends Thread {
        Handler handler = new Handler();

        @Override
        public void run() {
            while (!interrupted()) {

                // request data (until we reach our buffer limit)
                long bufferedPositionMs = mExoPlayer.getBufferedPosition();
                long currentPositionMs = mExoPlayer.getCurrentPosition();

                long bufferedMs = bufferedPositionMs - currentPositionMs;
                //Log.d(TAG, "ms buffered: " + bufferedMs);

                // buffer is full ? -> relax
                if(mSampleSource.isBufferFull()) {
                    try {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    continue;
                }

                // request next packet
                Packet req = mConnection.CreatePacket(ServerConnection.XVDR_RECSTREAM_GETPACKET, ServerConnection.XVDR_CHANNEL_REQUEST_RESPONSE);
                mConnection.transmitMessage(req);
            }
        }
    }

    private LoaderThread mLoader;
    private int mDurationInMs = 0;
    private long mLengthInBytes = 0;

    public RecordingPlayer(Context context, String server, String language, Listener listener) {
        super(context, server, language, listener);
    }

    public int openRecording(String recordingId) {
        Log.d(TAG, "open recording: " + recordingId);

        close();
        open();

        Packet req = mConnection.CreatePacket(ServerConnection.XVDR_RECSTREAM_OPEN, ServerConnection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(recordingId);

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            Log.e(TAG, "no response opening recording: " + recordingId);
            return NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status > 0) {
            Log.e(TAG, "error opening recording, status: " + status);
            return ERROR;
        }

        prepare();

        resp.getU32(); // 0
        mLengthInBytes = resp.getU64().longValue(); // length in bytes
        resp.getU8(); // TS / PES
        mDurationInMs = (int) resp.getU32() * 1000; // length in milliseconds

        Log.d(TAG, "length: " + mLengthInBytes + " bytes");
        Log.d(TAG, "duration: " + mDurationInMs / 1000 + " seconds");

        // set seekmap
        mSampleSource.setSeekMap(new SeekMap() {

            @Override
            public boolean isSeekable() {
                return true;
            }

            @Override
            public long getPosition(long timeUs) {
                return (mLengthInBytes * (timeUs / 1000)) / mDurationInMs;
            }
        });

        return SUCCESS;
    }

    public int getDurationMs() {
        return mDurationInMs;
    }

    public void seekTo(long timeMs) {
        // convert time to position
        long position = (mLengthInBytes * timeMs) / mDurationInMs;

        // stop playback
        onStopLoader();

        // seek
        Log.d(TAG, "seek to position: " + position + " / time: " + timeMs);
        super.seekTo(timeMs);

        // start playback
        onStartLoader();
    }

    protected void onStartLoader() {
        mLoader = new LoaderThread();
        mLoader.start();
    }

    protected void onStopLoader() {
        if(mLoader == null) {
            return;
        }

        mLoader.interrupt();

        try {
            mLoader.join(500);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        mLoader = null;
    }

}
