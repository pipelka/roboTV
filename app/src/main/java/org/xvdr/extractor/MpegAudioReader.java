package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.MpegAudioHeader;

import org.xvdr.audio.MpegAudioDecoder;
import org.xvdr.robotv.client.StreamBundle;

import java.nio.ByteBuffer;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";

    boolean hasOutputFormat = false;
    MpegAudioDecoder mDecoder;

    public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);
        mDecoder = new MpegAudioDecoder();
    }

    @Override
    public void consume(Allocation buffer) {
        ByteBuffer data = buffer.data();
        data.rewind();

        int length = mDecoder.decodeDirect(data, buffer.length());

        if(length == 0) {
            output.release(buffer);
            return;
        }

        Allocation chunk = output.allocate(length);
        chunk.timeUs = buffer.timeUs;
        chunk.flags = buffer.flags;

        if(!mDecoder.readDirect(chunk.data(), chunk.size())) {
            Log.e(TAG, "failed to read audio chunk");
            output.release(buffer);
            output.release(chunk);
            return;
        }

        chunk.setLength(length);
        output.release(buffer);

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

        output.sampleData(chunk);
    }

}