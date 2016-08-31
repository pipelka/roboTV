package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.Ac3Decoder;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;

    Ac3Decoder mDecoder;
    final byte[] decodeBuffer = new byte[64 * 1024];

    public Ac3Reader(PacketQueue output, StreamBundle.Stream stream, boolean ac3PassThrough, int channelConfiguration) {
        super(output, stream);
        this.ac3PassThrough = ac3PassThrough;

        if(ac3PassThrough) {
            Log.i(TAG, "AC3 passthrough enabled");
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

        int decoderMode = Ac3Decoder.layoutDolby;

        if(channelConfiguration == Player.CHANNELS_DIGITAL51) {
            decoderMode = Ac3Decoder.layout51;
        }
        else if(channelConfiguration == Player.CHANNELS_STEREO) {
            decoderMode = Ac3Decoder.layoutStereo;
        }

        mDecoder = new Ac3Decoder(decoderMode);
    }

    @Override
    public void consume(Packet p, int size, long timeUs, int flags) {
        if(ac3PassThrough) {
            output.sampleData(p, size, timeUs, flags);
            return;
        }

        int length = mDecoder.decode(p, size, decodeBuffer, 0, decodeBuffer.length);

        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
            return;
        }

        if(!output.hasFormat()) {
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
        }

        output.sampleData(decodeBuffer, length, timeUs, flags);
    }

}
