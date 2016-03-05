package org.xvdr.extractor;

import android.media.AudioFormat;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.ts.PtsTimestampAdjuster;
import com.google.android.exoplayer.upstream.Loader;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.Packet;
import org.xvdr.msgexchange.Session;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;

public class RoboTvSampleSource implements SampleSource, SampleSource.SampleSourceReader, Session.Callback, Loader.Callback {

    public interface Listener {

        void onTracksChanged(StreamBundle streamBundle);

        void onAudioTrackChanged(StreamBundle.Stream stream);

        void onVideoTrackChanged(StreamBundle.Stream stream);
    }

    protected class RoboTvLoadable implements Loader.Loadable {

        private volatile boolean mLoadCanceled = false;

        @Override
        public void cancelLoad() {
            mLoadCanceled = true;
        }

        @Override
        public boolean isLoadCanceled() {
            return mLoadCanceled;
        }

        @Override
        public void load() throws IOException, InterruptedException {
            while(!mLoadCanceled) {
                if(mOutputTracks[1].isFull() || mOutputTracks[0].isFull()) {
                    Thread.sleep(10);
                    continue;
                }

                if(!requestPacket()) {
                    Thread.sleep(10);
                }
            }
        }
    }

    final static private String TAG = "RoboTvSampleSource";

    final static private int TRACK_COUNT = 3;
    final static public int TRACK_VIDEO = 0;
    final static public int TRACK_AUDIO = 1;
    final static public int TRACK_SUBTITLE = 2;

    private AdaptiveAllocator mAllocator;
    private Connection mConnection;
    private StreamBundle mBundle;
    private Listener mListener;
    private Handler mHandler;
    private PtsTimestampAdjuster mTimestampAdjuster;
    private PlaybackSpeedAdjuster mPlaybackAdjuster;
    private Loader mLoader;

    private boolean mAudioPassthrough;

    private int mTrackCount = 0;
    private int mTracksEnabledCount = 0;
    private long mLargestParsedTimestampUs = Long.MIN_VALUE;
    private long mStreamPositionUs;
    private long mLastSeekPositionUs = 0;
    private int mChannelConfiguration;

    private long mCurrentPositionTimeshift;
    private long mStartPositionTimeshift;
    private long mSeekPosition = -1;

    final private int[] mPids = new int[TRACK_COUNT];
    final private boolean[] mNeedFormatChange = new boolean[TRACK_COUNT];
    final private PacketQueue[] mOutputTracks = new PacketQueue[TRACK_COUNT];
    private boolean[] mPendingDiscontinuities = new boolean[TRACK_COUNT];

    final private SparseArray<StreamReader> mStreamReaders = new SparseArray(TRACK_COUNT);
    final private SparseBooleanArray mTrackEnabled = new SparseBooleanArray(TRACK_COUNT);

    final private int mTrackContentMapping[] = {
        StreamBundle.CONTENT_VIDEO,
        StreamBundle.CONTENT_AUDIO,
        StreamBundle.CONTENT_SUBTITLE
    };

    private AudioCapabilities mAudioCapabilities;

    /**
     * Create a LiveTv SampleSource
     * @param connection the server connection to use
     */
    public RoboTvSampleSource(Connection connection) {
        this(connection, new Handler());
    }

    public RoboTvSampleSource(Connection connection, Handler handler) {
        this(connection, handler, null, false, Player.CHANNELS_SURROUND);
    }

    public RoboTvSampleSource(Connection connection, Handler handler, AudioCapabilities audioCapabilities, boolean audioPassthrough, int channelConfiguration) {
        mConnection = connection;
        mHandler = handler;
        mBundle = new StreamBundle();
        mAudioPassthrough = audioPassthrough;
        mChannelConfiguration = channelConfiguration;

        mTimestampAdjuster = new PtsTimestampAdjuster(PtsTimestampAdjuster.DO_NOT_OFFSET);
        mPlaybackAdjuster = new PlaybackSpeedAdjuster();

        mStartPositionTimeshift = System.currentTimeMillis();
        mCurrentPositionTimeshift = mStartPositionTimeshift;

        // create output tracks
        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i] = new PacketQueue();
            mPendingDiscontinuities[i] = false;
        }

        mAudioCapabilities = audioCapabilities;

        mLoader = new Loader("roboTV:streamloader");
        mAllocator = new AdaptiveAllocator(200, 32 * 1024);

        logChannelConfiguration(mAudioPassthrough, mChannelConfiguration);
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
        startLoading();

        // check if we have a video and a audio format
        for(int i = 0; i < 2; i++) {
            PacketQueue outputTrack = mOutputTracks[i];

            if(!outputTrack.hasFormat() || outputTrack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    protected void startLoading() {
        if(mLoader.isLoading()) {
            return;
        }

        Log.d(TAG, "startLoading");
        mLoader.startLoading(new RoboTvLoadable(), this);
    }

    protected void stopLoading() {
        if(!mLoader.isLoading()) {
            return;
        }

        Log.d(TAG, "stopLoading");
        mLoader.cancelLoading();
    }

    @Override
    public int getTrackCount() {
        return mTrackCount;
    }

    @Override

    synchronized public MediaFormat getFormat(int track) {
        return mOutputTracks[track].getFormat();
    }

    @Override

    synchronized public void enable(int track, long positionUs) {
        Log.d(TAG, "enable track: " + track);
        mTrackEnabled.put(track, true);
        mNeedFormatChange[track] = true;
        mStreamPositionUs = positionUs;

        mTracksEnabledCount++;

        if(mTracksEnabledCount == 1) {
            startLoading();
        }
    }

    // false - continue buffering
    // true - playback
    @Override
    public boolean continueBuffering(int track, long positionUs) {
        mStreamPositionUs = positionUs;

        return !mOutputTracks[track].isEmpty();
    }

    @Override
    public long readDiscontinuity(int track) {
        if(mPendingDiscontinuities[track]) {
            Log.d(TAG, "discontinuity read: track " + track);
            mPendingDiscontinuities[track] = false;
            return mLastSeekPositionUs;
        }

        return SampleSource.NO_DISCONTINUITY;
    }

    @Override
    public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder) {
        if(mPendingDiscontinuities[track]) {
            return NOTHING_READ;
        }

        mStreamPositionUs = positionUs;

        // get output track
        PacketQueue outputTrack = mOutputTracks[track];

        // get next packet
        PacketQueue.PacketHolder p = outputTrack.peek();

        if(p == null) {
            return NOTHING_READ;
        }

        // check if we have a queued format change
        if(p.isFormat()) {
            outputTrack.poll();
            formatHolder.format = p.format;
            mNeedFormatChange[track] = false;
            return FORMAT_READ;
        }

        // check if we need a format change (track enabled)
        if(mNeedFormatChange[track]) {
            if(outputTrack.hasFormat()) {
                formatHolder.format = outputTrack.getFormat();
                mNeedFormatChange[track] = false;
                return FORMAT_READ;
            }
            else {
                return NOTHING_READ;
            }
        }

        // check if we have sample data
        if(p.isSample()) {
            outputTrack.poll();

            Allocation buffer = p.buffer;

            // adpapt timestamps for playback speed
            sampleHolder.timeUs = mPlaybackAdjuster.adjustTimestamp(p.timeUs);
            sampleHolder.flags = p.flags;
            sampleHolder.size = buffer.length();

            sampleHolder.ensureSpaceForWrite(sampleHolder.size);
            sampleHolder.data.put(buffer.data(), 0, sampleHolder.size);

            mAllocator.release(buffer);
            return SAMPLE_READ;
        }

        return NOTHING_READ;
    }

    @Override
    public void seekToUs(long wallclockTimeMs) {
        if(mSeekPosition == wallclockTimeMs) {
            return;
        }

        Log.d(TAG, "seek to timestamp: " + wallclockTimeMs);
        Log.d(TAG, "current timestamp: " + mCurrentPositionTimeshift);
        Log.d(TAG, "seekbuffer start : " + mStartPositionTimeshift);
        Log.d(TAG, "seek difference  : " + (wallclockTimeMs - mCurrentPositionTimeshift) + " ms");

        mSeekPosition = wallclockTimeMs;
        mCurrentPositionTimeshift = mSeekPosition;

        stopLoading();
    }

    @Override
    public long getBufferedPositionUs() {
        return (mLargestParsedTimestampUs == Long.MIN_VALUE) ? mStreamPositionUs : mLargestParsedTimestampUs;
    }

    @Override
    public void disable(int track) {
        Log.d(TAG, "disable track: " + track);
        mTrackEnabled.put(track, false);
        mTracksEnabledCount--;

        if(mTracksEnabledCount == 0) {
            stopLoading();
        }
    }

    @Override
    public void release() {
        stopLoading();
        mLoader.release();
        mConnection.removeCallback(this);

        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i].clear();
        }
    }

    @Override
    public void onNotification(Packet packet) {
        int id = packet.getMsgID();

        switch(id) {
            case Connection.XVDR_STREAM_POSITIONS:
                long p  = packet.getS64();

                // sanity check
                if(p < mCurrentPositionTimeshift) {
                    mStartPositionTimeshift = p;
                }

                break;
        }
    }

    public void writePacket(final Packet packet) {
        switch(packet.getMsgID()) {
            case Connection.XVDR_STREAM_CHANGE:
                final StreamBundle newBundle = new StreamBundle();
                newBundle.updateFromPacket(packet);
                createOutputTracks(newBundle);
                break;

            case Connection.XVDR_STREAM_MUXPKT:
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

    private void addReader(int track, StreamBundle.Stream stream) {

        // create output track
        PacketQueue outputTrack = mOutputTracks[track];
        StreamReader reader = null;

        switch(stream.getMimeType()) {
            case MimeTypes.VIDEO_MPEG2:
                reader = new H262Reader(outputTrack, stream);
                break;

            case MimeTypes.VIDEO_H264:
                reader = new H264Reader(outputTrack, stream);
                break;

            case MimeTypes.VIDEO_H265:
                reader = new H265Reader(outputTrack, stream);
                break;

            case MimeTypes.AUDIO_AAC:
                reader = new AdtsReader(outputTrack, stream);
                break;

            case MimeTypes.AUDIO_AC3:
                boolean passthrough = mAudioPassthrough && mAudioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3);
                reader = new Ac3Reader(outputTrack, stream, passthrough, mChannelConfiguration, mAllocator);
                break;

            case MimeTypes.AUDIO_MPEG:
                reader = new MpegAudioReader(outputTrack, stream, false, mAllocator);
                break;
        }

        if(reader != null) {
            mStreamReaders.put(track, reader);
        }
    }

    private void createOutputTracks(StreamBundle newBundle) {

        // exit if the bundles are equal
        if(mBundle.isEqualTo(newBundle)) {
            return;
        }

        // post notification
        postTracksChanged(newBundle);

        mTrackCount = 0;

        // check for changed streams
        for(int i = 0; i < TRACK_COUNT; i++) {
            int pid = mPids[i];
            int index = newBundle.findIndexByPhysicalId(mTrackContentMapping[i], pid);

            if(index < 0) {
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
            }

            mPids[i] = stream.physicalId;
            mTrackCount++;

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
        if(mPids[TRACK_AUDIO] == pid) {
            return true;
        }

        StreamBundle.Stream stream = mBundle.getStreamOfPid(pid);

        if(stream == null) {
            return false;
        }

        mPids[TRACK_AUDIO] = stream.physicalId;
        addReader(TRACK_AUDIO, stream);

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

    private void skipPacketData(final Packet p) {
        p.getS64(); // pts
        p.getS64(); // dts
        p.getU32(); // duration
        int length = (int) p.getU32();
        p.skipBuffer(length);
        p.getS64(); // current timestamp
    }

    private void writeData(final Packet p) {
        // read pid of packet
        int pid = p.getU16();
        int track = findSampleReaderIndex(pid);

        if(track == -1) {
            skipPacketData(p);
            return;
        }

        if(mTrackEnabled.size() != 0 && !mTrackEnabled.get(track, false)) {
            skipPacketData(p);
            return;
        }

        StreamReader reader = mStreamReaders.get(track);

        // read packet properties

        long pts = p.getS64(); // pts
        long dts = p.getS64(); // dts
        p.getU32(); // duration
        int length = (int) p.getU32();

        // adjust first timestamp
        if(!mTimestampAdjuster.isInitialized()) {
            mTimestampAdjuster.adjustTimestamp(dts);
        }

        // adjust timestamp
        long timeUs = mTimestampAdjuster.adjustTimestamp(pts);

        // read buffer
        Allocation buffer = mAllocator.allocate(length);

        p.readBuffer(buffer.data(), 0, length);
        buffer.setLength(length);

        // read timestamp
        long pos = p.getS64(); // current timestamp

        // sanity check
        if(pos > mStartPositionTimeshift) {
            mCurrentPositionTimeshift = pos;
        }

        // push buffer to reader
        reader.consume(
            buffer,
            timeUs);

        mLargestParsedTimestampUs = Math.max(mLargestParsedTimestampUs, timeUs);
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

    private void logChannelConfiguration(boolean passthrough, int channelConfiguration) {
        Log.i(TAG, "audio: " +
              Player.nameOfChannelConfiguration(channelConfiguration) + " " +
              "passthrough: " + (passthrough ? "enabled" : "disabled"));
    }

    protected boolean requestPacket() {
        final Packet req = mConnection.CreatePacket(Connection.XVDR_CHANNELSTREAM_REQUEST, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);

        boolean keyFrameMode = mPlaybackAdjuster.getSpeed() > 2;
        req.putU8(keyFrameMode ? (short)1 : (short)0);

        final Packet resp = mConnection.transmitMessage(req);

        if(resp == null || resp.eop()) {
            if(resp != null) {
                resp.delete();
            }

            req.delete();
            return false;
        }

        // process all the packets in the packet
        while(!resp.eop()) {
            int msgId = resp.getU16();
            int clientId = resp.getU16();
            resp.setMsgID(msgId);
            resp.setClientID(clientId);

            if(resp.eop()) {
                Log.d(TAG, "error in packet skipping");
                return false;
            }

            writePacket(resp);
        }

        resp.delete();
        req.delete();

        return true;
    }

    public long getStartPositionWallclock() {
        return mStartPositionTimeshift;
    }

    public long getCurrentPositionWallclock() {
        return mCurrentPositionTimeshift;
    }

    public void setPlaybackSpeed(int speed) {
        mPlaybackAdjuster.setSpeed(speed, mStreamPositionUs);
    }

    // Loader callback

    @Override
    public void onLoadCanceled(Loader.Loadable loadable) {
        if(mSeekPosition != -1) {
            // set seek pointer
            Packet req = mConnection.CreatePacket(Connection.XVDR_CHANNELSTREAM_SEEK, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
            req.putS64(mSeekPosition);

            Packet resp = mConnection.transmitMessage(req);

            if(resp == null) {
                mSeekPosition = -1;
                startLoading();
                return;
            }

            mLastSeekPositionUs = mTimestampAdjuster.adjustTimestamp(resp.getS64());
            Log.d(TAG, "seek to position us: " + mLastSeekPositionUs);

            mCurrentPositionTimeshift = mSeekPosition;
            mLargestParsedTimestampUs = Long.MIN_VALUE;

            mPlaybackAdjuster.seek(mLastSeekPositionUs);

            // empty packet queue
            for(int i = 0; i < TRACK_COUNT; i++) {
                mPendingDiscontinuities[i] = true;
                mOutputTracks[i].clear();
            }

            mSeekPosition = -1;
            startLoading();
            return;
        }

        if(mTracksEnabledCount > 0) {
            startLoading();
        }
    }

    @Override
    public void onLoadCompleted(Loader.Loadable loadable) {
    }

    @Override
    public void onLoadError(Loader.Loadable loadable, IOException e) {
    }

}
