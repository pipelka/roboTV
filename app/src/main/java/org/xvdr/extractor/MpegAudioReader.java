package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.audio.MpegAudioDecoder;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.StreamBundle;

import java.nio.ByteBuffer;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";

    boolean hasOutputFormat = false;
    MpegAudioDecoder mDecoder;

    byte[] decodeBuffer;

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);
        mDecoder = new MpegAudioDecoder();
        decodeBuffer = new byte[128 * 1024];
    }

    @Override
    public void consume(Packet p, int size, long timeUs, int flags) {
        p.readBuffer(decodeBuffer, 0, size);
        int length = mDecoder.decode(decodeBuffer, 0, size);

        if(length == 0) {
            return;
        }

        if(!mDecoder.read(decodeBuffer, 0, length)) {
            Log.e(TAG, "failed to read audio chunk");
            return;
        }

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
                                     stream.language,
                                     C.ENCODING_PCM_16BIT);
            output.format(format);
            hasOutputFormat = true;
        }

        output.sampleData(decodeBuffer, length, timeUs, flags);
    }

}