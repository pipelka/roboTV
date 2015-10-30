package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.TrackOutput;
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

    MpegAudioDecoder mDecoder;

	public MpegAudioReader(TrackOutput output, StreamBundle.Stream stream) {
		super(output, stream);
        mDecoder = new MpegAudioDecoder();
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
        int length = mDecoder.decode(data.data, 0, data.capacity());

        if(length == 0) {
            return;
        }

        ParsableByteArray audioChunk = new ParsableByteArray(length);

        if(!mDecoder.read(audioChunk.data, 0, audioChunk.capacity())) {
            Log.e(TAG, "failed to read audio chunk");
            return;
        }

        if(!hasOutputFormat) {
            MediaFormat format = MediaFormat.createAudioFormat(
                    stream.physicalId, // < trackId
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

        output.sampleData(audioChunk, audioChunk.capacity());
        output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, audioChunk.capacity(), 0, null);
	}

}
