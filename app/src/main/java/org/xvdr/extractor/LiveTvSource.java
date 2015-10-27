package org.xvdr.extractor;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.msgexchange.Packet;
import org.xvdr.msgexchange.Session;
import org.xvdr.robotv.tv.ServerConnection;
import org.xvdr.robotv.tv.StreamBundle;

import java.io.IOException;

public class LiveTvSource implements SampleSource, SampleSource.SampleSourceReader, Session.Callback {

    public final static int NORESPONSE = -1;
    public final static int SUCCESS = 0;
    public final static int ERROR = 1;

    public interface Listener {

        void onTracksChanged(StreamBundle streamBundle);

    }

    final static private String TAG = "LiveTvSource";

    private ServerConnection mConnection;
    private StreamBundle mBundle;
    private Listener mListener;

    private SparseArray<DefaultTrackOutput> mSampleQueues = new SparseArray<>(16);
    private SparseArray<StreamReader> mSampleReaders = new SparseArray<>(16);
    private SparseBooleanArray mFormatSent = new SparseBooleanArray();
    private SparseBooleanArray mTrackEnabled = new SparseBooleanArray();

    private long offsetTimeUs = C.UNKNOWN_TIME_US;

    /**
     * Create a LiveTv SampleSource
     * @param connection the server connection to use
     */
    public LiveTvSource(ServerConnection connection) {
        mConnection = connection;
    }

    /**
     * Start streaming of a LiveTV channel with a default priority, without waiting for the first keyframe
     * @param channelUid the unique id of the channel
     * @return returns the status of the operation
     */
    public int openStream(int channelUid) {
        return openStream(channelUid, false);
    }

    /**
     * Start streaming of a LiveTV channel with a default priority
     * @param channelUid the unique id of the channel
     * @param waitForKeyFrame start streaming after the first IFRAME has been received
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, boolean waitForKeyFrame) {
        return openStream(channelUid, waitForKeyFrame, 50);
    }

    /**
     * Start streaming of a LiveTV channel
     * @param channelUid the unique id of the channel
     * @param waitForKeyFrame start streaming after the first IFRAME has been received
     * @param priority priority of the received device on the server
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, boolean waitForKeyFrame, int priority) {
        Packet req = mConnection.CreatePacket(ServerConnection.XVDR_CHANNELSTREAM_OPEN, ServerConnection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putU32(channelUid);
        req.putS32(priority); // priority 50
        req.putU8((short)(waitForKeyFrame ? 1 : 0)); // start with IFrame

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            return NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status > 0) {
            return ERROR;
        }

        return SUCCESS;
    }

    @Override
    public SampleSourceReader register() {
        mConnection.addCallback(this);
        return this;
    }

    @Override
    public void maybeThrowError() throws IOException {
    }

    @Override
    public boolean prepare(long position) {
        int trackCount = 0;

        synchronized (mSampleQueues) {
            if (mSampleQueues.size() == 0) {
                return false;
            }

            for (int i = 0; i < mSampleQueues.size(); i++) {
                DefaultTrackOutput outputTrack = mSampleQueues.valueAt(i);

                if (!outputTrack.hasFormat() || outputTrack.isEmpty()) {
                    return false;
                }

                trackCount++;
            }
        }

        return (trackCount != 0);
    }

    @Override
    public int getTrackCount() {
        synchronized (mSampleQueues) {
            return mSampleQueues.size();
        }
    }

    @Override
    public MediaFormat getFormat(int track) {
        DefaultTrackOutput outputTrack;

        synchronized (mSampleQueues) {
            outputTrack = mSampleQueues.get(track);
        }

        if(outputTrack == null || !outputTrack.hasFormat()) {
            return null;
        }

        return outputTrack.getFormat();
    }

    @Override
    public void enable(int track, long positionUs) {
        synchronized (mSampleQueues) {
            mFormatSent.put(track, false);
            mTrackEnabled.put(track, true);
        }
    }

    @Override
    public boolean continueBuffering(int track, long positionUs) {
        DefaultTrackOutput outputTrack;

        synchronized (mSampleQueues) {
            outputTrack = mSampleQueues.get(track);
            return outputTrack == null || !outputTrack.isEmpty();
        }
    }

    @Override
    public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder, boolean onlyReadDiscontinuity) {
        if(onlyReadDiscontinuity) {
            return NOTHING_READ;
        }

        synchronized (mSampleQueues) {

            // get output track
            DefaultTrackOutput outputTrack = mSampleQueues.get(track);
            if(outputTrack == null) {
                return NOTHING_READ;
            }

            // check if we should send a format change
            if (!mFormatSent.get(track, false) && outputTrack.hasFormat()) {
                formatHolder.format = outputTrack.getFormat();
                mFormatSent.put(track, true);
                return FORMAT_READ;
            }

            if (outputTrack.getSample(sampleHolder)) {
                return SAMPLE_READ;
            }
        }

        return NOTHING_READ;
    }

    @Override
    public void seekToUs(long positionUs) {
    }

    @Override
    public long getBufferedPositionUs() {
        return TrackRenderer.UNKNOWN_TIME_US;
    }

    @Override
    public void disable(int track) {
        synchronized (mSampleQueues) {
            mTrackEnabled.put(track, false);

            DefaultTrackOutput output = mSampleQueues.get(track);
            if (output != null) {
                output.clear();
            }
        }

    }

    @Override
    public void release() {
        mConnection.removeCallback(this);
    }

    @Override
    public void onNotification(Packet packet) {

        switch(packet.getMsgID()) {
            case ServerConnection.XVDR_STREAM_CHANGE:
                createOutputTracks(packet);
                break;
            case ServerConnection.XVDR_STREAM_MUXPKT:
                writeData(packet);
                break;
        }

    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onReconnect() {
    }

    /**
     * Set event listener.
     * @param listener the Listener interface receiving events
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public StreamBundle getStreamBundle() {
        return mBundle;
    }

    private void addReader(int track, StreamBundle.Stream stream) {

        // create output track
        DefaultTrackOutput outputTrack = new DefaultTrackOutput(new DefaultAllocator(8 * 1024));
        StreamReader reader = null;

        switch(stream.getMimeType()) {
            case MimeTypes.VIDEO_H264:
                reader = new H264Reader(outputTrack, stream);
                break;

            case MimeTypes.AUDIO_AAC:
                reader = new AdtsReader(outputTrack, stream);
                break;

            case MimeTypes.AUDIO_AC3:
                reader = new Ac3Reader(outputTrack, stream, false);
                break;
        }

        if(reader != null) {
            mSampleReaders.put(track, reader);
            mSampleQueues.put(track, outputTrack);
        }
    }

    private void createOutputTracks(Packet p) {
        mBundle = new StreamBundle();
        mBundle.updateFromPacket(p);

        Log.d(TAG, "createOutputTracks:" + mBundle.size() + " tracks");

        synchronized (mSampleQueues) {

            mSampleQueues.clear();
            mSampleReaders.clear();
            mFormatSent.clear();

            for (int i = 0; i < mBundle.size(); i++) {
                addReader(i, mBundle.valueAt(i));
            }
        }

        Log.d(TAG, mSampleReaders.size() + " readers created");

        if(mListener != null) {
            mListener.onTracksChanged(mBundle);
        }
    }

    private int findSampleReaderIndex(int physicalId) {

        for(int i = 0; i < mSampleReaders.size(); i++) {
            StreamReader reader = mSampleReaders.valueAt(i);
            if(reader.stream.physicalId == physicalId) {
                return mSampleReaders.keyAt(i);
            }
        }

        return -1;
    }

    private void writeData(Packet p) {
        // read pid of packet
        int pid = p.getU16();

        int track = findSampleReaderIndex(pid);

        synchronized (mSampleQueues) {
            if (mTrackEnabled.size() != 0 && !mTrackEnabled.get(track, false)) {
                return;
            }
        }

        StreamReader reader = mSampleReaders.get(track);
        if(reader == null) {
            return;
        }

        // read packet properties

        long pts = p.getS64();
        p.getS64(); // dts
        p.getU32(); // duration
        int length = (int) p.getU32();
        boolean isKeyFrame = (p.getClientID() == ServerConnection.IFRAME);

        // skip empty packet
        if(length == 0) {
            return;
        }

        // read buffer
        byte[] buffer = new byte[length];
        p.readBuffer(buffer, 0, length);

        // set time offset
        if(offsetTimeUs == C.UNKNOWN_TIME_US) {
            offsetTimeUs = pts;
        }

        // push buffer to reader
        synchronized (mSampleQueues) {
            reader.consume(
                    new ParsableByteArray(buffer),
                    pts - offsetTimeUs,
                    isKeyFrame);
        }
    }
}
