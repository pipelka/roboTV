package org.xvdr.extractor;

import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

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

        void onAudioTrackChanged(StreamBundle.Stream stream);

        void onVideoTrackChanged(StreamBundle.Stream stream);
    }

    final static private String TAG = "LiveTvSource";
    final static private int TRACK_COUNT = 3;
    final static private int TRACK_VIDEO = 0;
    final static private int TRACK_AUDIO = 1;
    final static private int TRACK_SUBTITLE = 2;

    private ServerConnection mConnection;
    private StreamBundle mBundle;
    private Listener mListener;
    private Handler mHandler;

    final private int[] mPids = new int[TRACK_COUNT];
    final private DefaultTrackOutput[] mOutputTracks = new DefaultTrackOutput[TRACK_COUNT];

    final private SparseArray<StreamReader> mStreamReaders = new SparseArray(TRACK_COUNT);
    final private SparseBooleanArray mFormatSent = new SparseBooleanArray(TRACK_COUNT);
    final private SparseBooleanArray mTrackEnabled = new SparseBooleanArray(TRACK_COUNT);

    final private int mTrackContentMapping[] = {
            StreamBundle.CONTENT_VIDEO,
            StreamBundle.CONTENT_AUDIO,
            StreamBundle.CONTENT_SUBTITLE
    };

    private long offsetTimeUs = C.UNKNOWN_TIME_US;

    /**
     * Create a LiveTv SampleSource
     * @param connection the server connection to use
     */
    public LiveTvSource(ServerConnection connection) {
        this(connection, new Handler());
    }

    public LiveTvSource(ServerConnection connection, Handler handler) {
        mConnection = connection;
        mHandler = handler;
        mBundle = new StreamBundle();

        // create output tracks
        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i] = new DefaultTrackOutput(new DefaultAllocator(16 * 1024));
        }
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
    synchronized public boolean prepare(long position) {
        // check if we have a video and a audio format
        for (int i = 0; i < 2; i++) {
            DefaultTrackOutput outputTrack = mOutputTracks[i];

            if (!outputTrack.hasFormat() || outputTrack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    synchronized public int getTrackCount() {
        return mStreamReaders.size();
    }

    @Override
    synchronized public MediaFormat getFormat(int track) {
        DefaultTrackOutput outputTrack = mOutputTracks[track];
        if(!outputTrack.hasFormat()) {
            return null;
        }

        return outputTrack.getFormat();
    }

    @Override
    synchronized public void enable(int track, long positionUs) {
        Log.d(TAG, "enable track: " + track);
        mFormatSent.put(track, false);
        mTrackEnabled.put(track, true);
    }

    // false - continue buffering
    // true - playback
    @Override
    synchronized  public boolean continueBuffering(int track, long positionUs) {
        if(!mFormatSent.get(track, false)) {
            return false;
        }

        return !mOutputTracks[track].isEmpty();
    }

    @Override
    synchronized public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder, boolean onlyReadDiscontinuity) {
        if(onlyReadDiscontinuity) {
            return NOTHING_READ;
        }

        // get output track
        DefaultTrackOutput outputTrack = mOutputTracks[track];
        if(outputTrack == null) {
            return NOTHING_READ;
        }

        // check if we should send a format change
        if (!mFormatSent.get(track, false) && outputTrack.hasFormat()) {
            formatHolder.format = outputTrack.getFormat();
            mFormatSent.put(track, true);
            return FORMAT_READ;
        }

        if (outputTrack.getSample(sampleHolder) && mFormatSent.get(track, false)) {
            return SAMPLE_READ;
        }

        return NOTHING_READ;
    }

    @Override
    public void seekToUs(long positionUs) {
        Log.d(TAG, "seekToUs: " + positionUs);
    }

    @Override
    synchronized public long getBufferedPositionUs() {
        long largestParsedTimestampUs = Long.MIN_VALUE;

        for (int i = 0; i < TRACK_COUNT; i++) {
            DefaultTrackOutput output = mOutputTracks[i];
            if(output != null) {
                largestParsedTimestampUs = Math.max(largestParsedTimestampUs, output.getLargestParsedTimestampUs());
            }
        }

        return (largestParsedTimestampUs == Long.MIN_VALUE) ? TrackRenderer.UNKNOWN_TIME_US : largestParsedTimestampUs;
    }

    @Override
    synchronized public void disable(int track) {
        Log.d(TAG, "disable track: " + track);
        mTrackEnabled.put(track, false);
    }

    @Override
    public void release() {
        mConnection.removeCallback(this);
    }

    @Override
    public void onNotification(Packet packet) {

        switch(packet.getMsgID()) {
            case ServerConnection.XVDR_STREAM_CHANGE:
                final StreamBundle newBundle = new StreamBundle();
                newBundle.updateFromPacket(packet);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        createOutputTracks(newBundle);

                    }
                });
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

    synchronized public StreamBundle getStreamBundle() {
        return mBundle;
    }

    private void addReader(int track, StreamBundle.Stream stream) {

        // create output track
        DefaultTrackOutput outputTrack = mOutputTracks[track];
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

            case MimeTypes.AUDIO_MPEG:
                reader = new MpegAudioReader(outputTrack, stream);
                break;
        }

        if(reader != null) {
            mStreamReaders.put(track, reader);
        }
    }

    synchronized private void createOutputTracks(StreamBundle newBundle) {

        // exit if the bundles are equal
        if(mBundle.isEqualTo(newBundle)) {
            return;
        }

        // post notification
        postTracksChanged(newBundle);

        // check for changed streams
        for(int i = 0; i < TRACK_COUNT; i++) {
            int pid = mPids[i];
            int index = newBundle.findIndexByPhysicalId(mTrackContentMapping[i], pid);

            if (index < 0) {
                index = 0;
            }

            // get old stream
            StreamBundle.Stream oldStream = mBundle.getStreamOfPid(mPids[i]);

            // get new stream
            StreamBundle.Stream stream = newBundle.getStream(mTrackContentMapping[i], index);

            // skip missing streams
            if(stream == null) {
                // disable if track was enabled
                if(mTrackEnabled.get(i, false)) {
                    mTrackEnabled.put(i, false);
                }
                continue;
            }

            // old stream did not exist -> create new stream
            // or check if the stream has changed
            if(oldStream == null || !stream.isEqualTo(oldStream)) {
                addReader(i, stream);
                mFormatSent.put(i, false);
            }

            mPids[i] = stream.physicalId;

            if(i == TRACK_AUDIO) {
                postAudioTrackChanged(stream);
            }
            else if(i == TRACK_VIDEO) {
                postVideoTrackChanged(stream);
            }
        }

        mBundle = newBundle;
    }

    synchronized public boolean selectAudioTrack(int pid) {
        // pid already selected
        if (mPids[TRACK_AUDIO] == pid) {
            return true;
        }

        StreamBundle.Stream stream = mBundle.getStreamOfPid(pid);

        if (stream == null) {
            return false;
        }

        mPids[TRACK_AUDIO] = stream.physicalId;
        addReader(TRACK_AUDIO, stream);
        mFormatSent.put(TRACK_AUDIO, false);

        postAudioTrackChanged(stream);
        return true;
    }

    private int findSampleReaderIndex(int physicalId) {

        for(int i = 0; i < mStreamReaders.size(); i++) {
            StreamReader reader = mStreamReaders.valueAt(i);
            if(reader.stream.physicalId == physicalId) {
                return mStreamReaders.keyAt(i);
            }
        }

        return -1;
    }

    private void writeData(Packet p) {
        // read pid of packet
        int pid = p.getU16();
        int track = findSampleReaderIndex(pid);

        if(track == -1) {
            return;
        }

        if (mTrackEnabled.size() != 0 && !mTrackEnabled.get(track, false)) {
            return;
        }

        StreamReader reader = mStreamReaders.get(track);

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

        long timeUs = pts - offsetTimeUs;

        // skip all packets before our starttime
        if(timeUs < 0) {
            return;
        }

        // push buffer to reader
        reader.consume(
                new ParsableByteArray(buffer),
                timeUs,
                isKeyFrame);
    }

    private void postTracksChanged(final StreamBundle bundle) {
        if(mListener == null) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onTracksChanged(bundle);
            }
        });
    }

    private void postAudioTrackChanged(final StreamBundle.Stream stream) {
        if(mListener == null) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onAudioTrackChanged(stream);
            }
        });
    }

    private void postVideoTrackChanged(final StreamBundle.Stream stream) {
        if(mListener == null) {
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onVideoTrackChanged(stream);
            }
        });
    }
}
