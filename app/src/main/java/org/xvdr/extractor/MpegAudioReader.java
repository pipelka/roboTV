package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.MpegAudioDecoder;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";
    MpegAudioDecoder mDecoder;

    byte[] decodeBuffer;

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);
        mDecoder = new MpegAudioDecoder();
        decodeBuffer = new byte[64 * 1024];
    }

    @Override
    public void consume(Packet p, int size, long timeUs, int flags) {
        int length = mDecoder.decode(p, size, decodeBuffer, 0, decodeBuffer.length);

        if(length == 0) {
            return;
        }

        if(!output.hasFormat()) {
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