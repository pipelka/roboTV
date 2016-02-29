package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.audio.AC3Decoder;
import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    private boolean hasOutputFormat = false;

    AdaptiveAllocator mAllocator;
    AC3Decoder mDecoder;

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, AdaptiveAllocator allocator) {
        this(output, stream, false, allocator);
    }

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough, AdaptiveAllocator allocator) {
        this(output, stream, ac3PassThrough, Player.CHANNELS_SURROUND, allocator);
    }

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough, int channelConfiguration, AdaptiveAllocator allocator) {
        super(output, stream);
        this.ac3PassThrough = ac3PassThrough;
        mAllocator = allocator;

        if(ac3PassThrough) {
            output.format(MediaFormat.createAudioFormat(
                              Integer.toString(stream.physicalId),
                              MimeTypes.AUDIO_AC3,
                              stream.bitRate,
                              MediaFormat.NO_VALUE,
                              C.UNKNOWN_TIME_US,
                              stream.channels,
                              stream.sampleRate,
                              null,
                              stream.language));
            return;
        }

        int decoderMode = AC3Decoder.LayoutDolby;

        if(channelConfiguration == Player.CHANNELS_DIGITAL51) {
            decoderMode = AC3Decoder.Layout51;
        }
        else if(channelConfiguration == Player.CHANNELS_STEREO) {
            decoderMode = AC3Decoder.LayoutStereo;
        }

        mDecoder = new AC3Decoder(decoderMode);
    }

    @Override
    public void consume(Allocation buffer, long pesTimeUs) {
        if(ac3PassThrough) {
            output.sampleData(buffer, pesTimeUs, C.SAMPLE_FLAG_SYNC);
            return;
        }

        int length = mDecoder.decode(buffer.data(), 0, buffer.length());

        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
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
            Log.i(TAG, "channels: " + mDecoder.getChannels());
            MediaFormat format = MediaFormat.createAudioFormat(
                                     Integer.toString(stream.physicalId), // < trackId
                                     MimeTypes.AUDIO_RAW,
                                     mDecoder.getBitRate(),
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

}
