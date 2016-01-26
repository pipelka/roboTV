package org.xvdr.extractor;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
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
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.Packet;
import org.xvdr.msgexchange.Session;
import org.xvdr.robotv.tv.ServerConnection;
import org.xvdr.robotv.tv.StreamBundle;

import java.io.IOException;

public class RoboTvSampleSource implements SampleSource, SampleSource.SampleSourceReader, Session.Callback {

    public interface Listener {

        void onTracksChanged(StreamBundle streamBundle);

        void onAudioTrackChanged(StreamBundle.Stream stream);

        void onVideoTrackChanged(StreamBundle.Stream stream);
    }

    final static private String TAG = "RoboTvSampleSource";
    final static private int TRACK_COUNT = 3;
    final static private int TRACK_VIDEO = 0;
    final static private int TRACK_AUDIO = 1;
    final static private int TRACK_SUBTITLE = 2;

    private ServerConnection mConnection;
    private StreamBundle mBundle;
    private Listener mListener;
    private Handler mHandler;
    private PtsTimestampAdjuster mTimestampAdjuster;
    private boolean mAudioPassthrough;

    private int mTrackCount = 0;
    private long mLargestParsedTimestampUs = Long.MIN_VALUE;
    private long streamPositionUs;
    private int mChannelConfiguration;

    final private int[] mPids = new int[TRACK_COUNT];
    final private boolean[] mNeedFormatChange = new boolean[TRACK_COUNT];
    final private PacketQueue[] mOutputTracks = new PacketQueue[TRACK_COUNT];

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
    public RoboTvSampleSource(ServerConnection connection) {
        this(connection, new Handler());
    }

    public RoboTvSampleSource(ServerConnection connection, Handler handler) {
        this(connection, handler, null, false, Player.CHANNELS_SURROUND);
    }

    public RoboTvSampleSource(ServerConnection connection, Handler handler, AudioCapabilities audioCapabilities, boolean audioPassthrough, int channelConfiguration) {
        mConnection = connection;
        mHandler = handler;
        mBundle = new StreamBundle();
        mAudioPassthrough = audioPassthrough;
        mChannelConfiguration = channelConfiguration;

        mTimestampAdjuster = new PtsTimestampAdjuster(PtsTimestampAdjuster.DO_NOT_OFFSET);

        // create output tracks
        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i] = new PacketQueue();
        }

        mAudioCapabilities = audioCapabilities;

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
        // check if we have a video and a audio format
        for (int i = 0; i < 2; i++) {
            PacketQueue outputTrack = mOutputTracks[i];

            if (!outputTrack.hasFormat() || outputTrack.isEmpty()) {
                return false;
            }
        }

        return true;
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
        streamPositionUs = positionUs;
    }

    // false - continue buffering
    // true - playback
    @Override
    public boolean continueBuffering(int track, long positionUs) {
        streamPositionUs = positionUs;
        return !mOutputTracks[track].isEmpty();
    }

    @Override
    public long readDiscontinuity(int track) {
        return SampleSource.NO_DISCONTINUITY;
    }

    @Override
    public int readData(int track, long positionUs, MediaFormatHolder formatHolder, SampleHolder sampleHolder) {
        streamPositionUs = positionUs;

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
            sampleHolder.timeUs = p.timeUs;
            sampleHolder.flags = p.flags;
            sampleHolder.size = p.length;

            sampleHolder.ensureSpaceForWrite(sampleHolder.size);
            sampleHolder.data.put(p.data, 0, sampleHolder.size);

            return SAMPLE_READ;
        }

        return NOTHING_READ;
    }

    @Override
    public void seekToUs(long positionUs) {
        Log.d(TAG, "seekToUs: " + positionUs);

        // empty packet queue
        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i].clear();
        }
        streamPositionUs = positionUs;
        mLargestParsedTimestampUs = Long.MIN_VALUE;
    }

    @Override
    public long getBufferedPositionUs() {
        return (mLargestParsedTimestampUs == Long.MIN_VALUE) ? streamPositionUs : mLargestParsedTimestampUs;
    }

    public long getStreamPositionUs() {
        return streamPositionUs;
    }

    @Override
    public void disable(int track) {
        Log.d(TAG, "disable track: " + track);
        mTrackEnabled.put(track, false);
    }

    @Override
    public void release() {
        mConnection.removeCallback(this);

        for(int i = 0; i < TRACK_COUNT; i++) {
            mOutputTracks[i].clear();
        }
    }

    @Override
    public void onNotification(Packet packet) {
        // process only STATUS messages
        if(packet.getType() != ServerConnection.XVDR_CHANNEL_STREAM) {
            return;
        }

        switch(packet.getMsgID()) {
            case ServerConnection.XVDR_STREAM_CHANGE:
                final StreamBundle newBundle = new StreamBundle();
                newBundle.updateFromPacket(packet);
                createOutputTracks(newBundle);
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

    private void addReader(int track, StreamBundle.Stream stream) {

        // create output track
        PacketQueue outputTrack = mOutputTracks[track];
        StreamReader reader = null;

        switch(stream.getMimeType()) {
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
                reader = new Ac3Reader(outputTrack, stream, passthrough, mChannelConfiguration);
                break;

            case MimeTypes.AUDIO_MPEG:
                reader = new MpegAudioReader(outputTrack, stream, false);
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

        // create new streams
        if(mBundle.size() != newBundle.size()) {
            Log.d(TAG, "number of streams differ - resetting stream bundle");
            mBundle.clear();
            mStreamReaders.clear();

            for(int i=0; i < TRACK_COUNT; i++) {
                mPids[i] = 0;
            }
        }

        // check for changed streams
        for (int i = 0; i < TRACK_COUNT; i++) {
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
            if (stream == null) {
                // disable if track was enabled
                if (mTrackEnabled.get(i, false)) {
                    mTrackEnabled.put(i, false);
                }
                continue;
            }

            // old stream did not exist -> create new stream
            // or check if the stream has changed
            if (oldStream == null || !stream.isEqualTo(oldStream)) {
                addReader(i, stream);
            }

            mPids[i] = stream.physicalId;
            mTrackCount++;

            if (i == TRACK_AUDIO) {
                postAudioTrackChanged(stream);
            } else if (i == TRACK_VIDEO) {
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

        // read packet properties

        long pts = p.getS64(); // pts
        long dts = p.getS64(); // dts
        p.getU32(); // duration
        int length = (int) p.getU32();
        boolean isKeyFrame = (p.getClientID() == ServerConnection.IFRAME);

        // skip empty packet
        if(length == 0) {
            return;
        }

        // adjust first timestamp
        if(!mTimestampAdjuster.isInitialized()) {
            mTimestampAdjuster.adjustTimestamp(dts);
        }

        // adjust timestamp
        long timeUs = mTimestampAdjuster.adjustTimestamp(pts);

        // read buffer
        byte[] buffer = new byte[length];
        p.readBuffer(buffer, 0, length);

        // push buffer to reader
        reader.consume(
                buffer,
                timeUs,
                isKeyFrame);

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
}
