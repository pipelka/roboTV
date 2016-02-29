package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.MpegAudioHeader;

import org.xvdr.audio.MpegAudioDecoder;
import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";
    boolean hasOutputFormat = false;

    AdaptiveAllocator mAllocator;
    MpegAudioDecoder mDecoder;

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream, AdaptiveAllocator allocator) {
        this(output, stream, false, allocator);
    }

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream, boolean useHwDecoder, AdaptiveAllocator allocator) {
        super(output, stream);
        mAllocator = allocator;

        if(!useHwDecoder) {
            mDecoder = new MpegAudioDecoder();
        }
    }

    @Override
    public void consume(Allocation buffer, long pesTimeUs) {
        if(mDecoder == null) {
            consumeHw(buffer, pesTimeUs);
            return;
        }

        int length = mDecoder.decode(buffer.data(), 0, buffer.length());

        if(length == 0) {
            mAllocator.release(buffer);
            return;
        }

        Allocation chunk = mAllocator.allocate(length);

        if(!mDecoder.read(chunk.data(), 0, chunk.size())) {
            Log.e(TAG, "failed to read audio chunk");
            mAllocator.release(buffer);
            mAllocator.release(chunk);
            return;
        }

        chunk.setLength(length);
        mAllocator.release(buffer);

        if(!hasOutputFormat) {
            MediaFormat format = MediaFormat.createAudioFormat(
                                     Integer.toString(stream.physicalId), // < trackId
                                     MimeTypes.AUDIO_RAW,
                                     MediaFormat.NO_VALUE,
                                     MediaFormat.NO_VALUE,
                                     C.UNKNOWN_TIME_US,
                                     mDecoder.getChannels(),
                                     mDecoder.getSampleRate(),
                                     null,
                                     stream.language);
            output.format(format);
            hasOutputFormat = true;
        }

        output.sampleData(chunk, pesTimeUs, C.SAMPLE_FLAG_SYNC);
    }

    private void consumeHw(Allocation buffer, long pesTimeUs) {
        byte[] data = buffer.data();

        if(!hasOutputFormat) {
            int  header = ((data[0] << 24) | (data[1] << 16) | (data[2] <<  8) | data[3]);
            MpegAudioHeader mpegAudioHeader = new MpegAudioHeader();

            if(MpegAudioHeader.populateHeader(header, mpegAudioHeader)) {
                MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                                              Integer.toString(stream.physicalId), // < trackId
                                              mpegAudioHeader.mimeType,
                                              mpegAudioHeader.bitrate,
                                              MpegAudioHeader.MAX_FRAME_SIZE_BYTES,
                                              C.UNKNOWN_TIME_US,
                                              mpegAudioHeader.channels,
                                              mpegAudioHeader.sampleRate,
                                              null,
                                              stream.language);

                output.format(mediaFormat);
                hasOutputFormat = true;
            }
        }

        output.sampleData(buffer, pesTimeUs, C.SAMPLE_FLAG_SYNC);
    }

}