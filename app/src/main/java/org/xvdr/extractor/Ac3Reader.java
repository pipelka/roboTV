package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.audio.AC3Decoder;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    private boolean hasOutputFormat = false;

    byte[] decodeBuffer;

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
                              MediaFormat.NO_VALUE,
                              MediaFormat.NO_VALUE,
                              C.UNKNOWN_TIME_US,
                              stream.channels,
                              stream.sampleRate,
                              null,
                              stream.language));
            return;
        }

        decodeBuffer = new byte[64 * 1024];
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
    public void consume(Packet p, int size, long timeUs, int flags) {
        if(ac3PassThrough) {
            output.sampleData(p, size, timeUs, flags);
            return;
        }

        p.readBuffer(decodeBuffer, 0, size);
        int length = mDecoder.decode(decodeBuffer, 0, size);

        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
            return;
        }

        if(length > decodeBuffer.length) {
            Log.e(TAG, "decode buffer too small !!!");
            return;
        }

        if(!mDecoder.read(decodeBuffer, 0, length)) {
            Log.e(TAG, "failed to read audio chunk");
            return;
        }

        if(!hasOutputFormat) {
            Log.i(TAG, "channels: " + mDecoder.getChannels());
            MediaFormat format = MediaFormat.createAudioFormat(
                                     Integer.toString(stream.physicalId), // < trackId
                                     MimeTypes.AUDIO_RAW,
                                     MediaFormat.NO_VALUE,
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

        output.sampleData(decodeBuffer, length, timeUs, flags);
    }

}
