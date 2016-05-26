package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.audio.AC3Decoder;
import org.xvdr.robotv.client.StreamBundle;

import java.nio.ByteBuffer;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    private boolean hasOutputFormat = false;

    AC3Decoder mDecoder;

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream) {
        this(output, stream, false);
    }

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough) {
        this(output, stream, ac3PassThrough, Player.CHANNELS_SURROUND);
    }

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough, int channelConfiguration) {
        super(output, stream);
        this.ac3PassThrough = ac3PassThrough;

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
    public void consume(SampleBuffer buffer) {
        if(ac3PassThrough) {
            output.sampleData(buffer);
            return;
        }

        ByteBuffer data = buffer.data();
        data.rewind();

        int length = mDecoder.decodeDirect(data, buffer.limit());

        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
            return;
        }

        SampleBuffer chunk = output.allocate(length);
        chunk.timeUs = buffer.timeUs;
        chunk.flags = buffer.flags;

        if(!mDecoder.readDirect(chunk.data(), chunk.capacity())) {
            Log.e(TAG, "failed to read audio chunk");
            output.release(buffer);
            output.release(chunk);
            return;
        }

        chunk.setLength(length);
        output.release(buffer);

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
                                     stream.language,
                                     C.ENCODING_PCM_16BIT);
            output.format(format);
            hasOutputFormat = true;
        }

        output.sampleData(chunk);
    }

}
