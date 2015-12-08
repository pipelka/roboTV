package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.audio.MpegAudioDecoder;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR MPEG Audio stream.
 */
final class MpegAudioReader extends StreamReader {

    private static final String TAG = "MpegAudioReader";
    boolean hasOutputFormat = false;

    private static final int MAX_CHUNK_SIZE = 64 * 1024;

    MpegAudioDecoder mDecoder;

	public MpegAudioReader(PacketQueue output, StreamBundle.Stream stream) {
		super(output, stream);
        mDecoder = new MpegAudioDecoder();
	}

	@Override
	public void consume(byte[] data, long pesTimeUs, boolean isKeyframe) {
        byte[] buffer= new byte[MAX_CHUNK_SIZE];
        mDecoder.setDecodeBuffer(buffer, 0, MAX_CHUNK_SIZE);

        int length = mDecoder.decode(data, 0, data.length);

        if(length == 0) {
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
                    stream.language);
            output.format(format);
            hasOutputFormat = true;
        }

        output.sampleData(buffer, length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
	}

}
