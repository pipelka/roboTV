package org.xvdr.player.extractor;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.TimestampAdjuster;

import org.xvdr.player.source.PositionReference;
import org.robotv.client.Connection;
import org.xvdr.player.StreamBundle;

import java.io.IOException;

public class RoboTvExtractor implements Extractor {

    private static final String TAG = RoboTvExtractor.class.getName();

    public static class Factory implements ExtractorsFactory {

        private RoboTvExtractor extractor = null;
        final private Listener listener;
        final private PositionReference position;
        final private String audioLanguage;
        final private boolean audioPassthrough;

        public Factory(PositionReference position, Listener listener, String audioLanguage, boolean audioPassthrough) {
            this.listener = listener;
            this.position = position;
            this.audioLanguage = audioLanguage;
            this.audioPassthrough = audioPassthrough;
        }

        @Override
        public Extractor[] createExtractors() {
            extractor = new RoboTvExtractor(position, listener, audioLanguage, audioPassthrough);
            return new Extractor[] { extractor };
        }

        public void selectAudioTrack(String trackId) {
            extractor.selectAudioTrack(trackId);
        }
    }

    private class PositionSeekMap implements SeekMap {
        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long getDurationUs() {
            return C.TIME_UNSET;
        }

        @Override
        public long getPosition(long timeUs) {
            return position.positionFromTimeUs(timeUs);
        }
    }

    public interface Listener {
        void onTracksChanged(StreamBundle bundle);
        void onAudioTrackChanged(Format format);
    }

    private ExtractorOutput output;
    private boolean seenFirstDts;
    private StreamManager streamManager;
    private int nextAudioPid;

    private long seekTimeUs = 0;
    private long seekOffsetUs = 0;

    final private ExtractorBufferPacket scratch;
    final private Listener listener;
    final private PositionReference position;
    final private TimestampAdjuster timestampAdjuster;
    final private String audioLanguage;
    final private boolean audioPassthrough;

    private RoboTvExtractor(PositionReference position, Listener listener, String audioLanguage, boolean audioPassthrough) {
        this.listener = listener;
        this.position = position;
        this.scratch = new ExtractorBufferPacket(new byte[1024]);
        this.timestampAdjuster = new TimestampAdjuster(TimestampAdjuster.DO_NOT_OFFSET);
        this.seenFirstDts = false;
        this.audioLanguage = audioLanguage;
        this.audioPassthrough = audioPassthrough;
        this.nextAudioPid = 0;
    }

    @Override
    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        return true;
    }

    @Override
    public void init(ExtractorOutput output) {
        this.output = output;
        output.seekMap(new PositionSeekMap());
    }

    @Override
    synchronized public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {

        // check for audio track switch
        if(nextAudioPid != 0 && streamManager != null) {
            Format format = streamManager.selectAudioTrack(nextAudioPid);
            if(listener != null && format != null) {
                listener.onAudioTrackChanged(format);
            }
            nextAudioPid = 0;
            return RESULT_CONTINUE;
        }

        // read packet header

        // messageId  uint16  2 bytes
        // frameType  uint16  2 bytes
        // length     uint32  4 bytes

        scratch.read(input, 4);

        int messageId = scratch.getU16();
        boolean keyFrame = (scratch.getU16() == Connection.IFRAME);

        // check for format packet
        if(messageId == Connection.XVDR_STREAM_CHANGE) {
            Log.d(TAG, "stream change packet received");
            scratch.peek(input, 512);
            updateStreams(scratch);

            int bytesRead = scratch.position();
            Log.d(TAG, "skipping " + bytesRead + " bytes (stream change packet)");
            input.skipFully(bytesRead);

            return RESULT_CONTINUE;
        }

        // exit if we didn't receive a stream packet
        if(messageId != Connection.XVDR_STREAM_MUXPKT) {
            Log.e(TAG, "unknown message id: " + messageId + " - skipping packet");
            input.skip(1024*1024);
            return RESULT_CONTINUE;
        }

        // read stream packet header

        // pid        uint16  2 bytes
        // pts        int64   8 bytes
        // dts        int64   8 bytes
        // duration   uint32  4 bytes
        // length     uint32  4 bytes

        scratch.read(input, 26);

        int pid = scratch.getU16();
        long pts = scratch.getS64();
        long dts = scratch.getS64();
        scratch.getU32();

        int size = (int) scratch.getU32(); // size of encapsulated stream data

        if(streamManager == null) {
            input.skipFully(size);
            input.skipFully(8);
            return RESULT_CONTINUE;
        }

        StreamBundle.Stream stream = streamManager.getStream(pid);
        TrackOutput output = streamManager.getOutput(stream);

        // unknown stream ?
        if(output == null) {
            input.skipFully(size);
            input.skipFully(8);
            return RESULT_CONTINUE;
        }

        if(!seenFirstDts) {
            timestampAdjuster.adjustTsTimestamp(dts);
            seenFirstDts = true;
        }

        // convert PTS -> timeUs
        long timeUs = timestampAdjuster.adjustTsTimestamp(pts);

        // compute the time gap caused by seek
        if(seekTimeUs != 0) {
            seekOffsetUs = timeUs - seekTimeUs;
            seekTimeUs = 0;
            Log.d(TAG, "seek offset: " + (seekOffsetUs / 1000) + " ms");
        }

        timeUs -= seekOffsetUs;

        // audio track timestamp synchronization (32ms)
        // from somewhere we get this timestamp difference
        // by now we adjust this empirically
        if(stream.isAudio()) {
            timeUs += 32000;
        }

        int flags =
                stream.isVideo() ? (keyFrame ? C.BUFFER_FLAG_KEY_FRAME : 0) :
                stream.isAudio() ? C.BUFFER_FLAG_KEY_FRAME : 0;

        // consume stream data
        if(timeUs >= 0) {
            int length = size;

            while(length > 0) {
                length -= output.sampleData(input, length, false);
            }

            output.sampleMetadata(timeUs, flags, size, 0, null);
        }

        // get current position
        // position   uint64  8 bytes

        scratch.read(input, 8);

        long pos = scratch.getU64();

        // sanity check if position is within the range
        if(position.getStartPosition() < pos && pos < position.getEndPosition() && timeUs >= 0) {
            position.set(timeUs, pos);
        }

        return RESULT_CONTINUE;
    }

    @Override
    synchronized public void seek(long p, long timeUs) {
        Log.d(TAG, "seek: " + p);
        seekTimeUs = timeUs;
        timestampAdjuster.reset();
    }

    @Override
    public void release() {
        this.seenFirstDts = false;
    }

    private void selectAudioTrack(String trackId) {
        int pid = 0;

        try {
            pid = Integer.parseInt(trackId);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        nextAudioPid = pid;
    }

    private void updateStreams(BufferPacket p) {
        final StreamBundle bundle = new StreamBundle();

        if(!bundle.updateFromPacket(p)) {
            return;
        }

        Log.d(TAG, "create streams");
        this.streamManager = new StreamManager(bundle);
        streamManager.createStreams(output, audioLanguage, audioPassthrough);

        if(listener != null) {
            listener.onTracksChanged(bundle);
            listener.onAudioTrackChanged(streamManager.getAudioFormat());
        }
    }

}
