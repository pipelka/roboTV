package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.audio.AC3DecoderNative;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    boolean hasOutputFormat = false;

    AC3DecoderNative mDecoder;

	public Ac3Reader(TrackOutput output, StreamBundle.Stream stream, boolean ac3PassThrough) {
		super(output, stream);
        this.ac3PassThrough = ac3PassThrough;

        if(ac3PassThrough) {
            output.format(MediaFormat.createAudioFormat(
                    stream.physicalId,
                    MimeTypes.AUDIO_AC3,
                    stream.bitRate,
                    -1,
                    C.UNKNOWN_TIME_US,
                    stream.channels,
                    stream.sampleRate,
                    null,
                    stream.language));
            return;
        }

        mDecoder = new AC3DecoderNative(AC3DecoderNative.layoutStereo);
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
        if(ac3PassThrough) {
            output.sampleData(data, data.capacity());
            output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, data.capacity(), 0, null);
            return;
        }

        int decodedLength = mDecoder.decode(data.data, 0, data.capacity());
        if(decodedLength == 0) {
            Log.e(TAG, "Unable to decode frame data");
            return;
        }

        ParsableByteArray audioChunk = new ParsableByteArray(decodedLength);

        if(!mDecoder.getOutput(audioChunk.data, 0, audioChunk.capacity())) {
            Log.e(TAG, "failed to get output buffer");
            return;
        }

        if(!hasOutputFormat) {
            MediaFormat format = MediaFormat.createAudioFormat(
                    stream.physicalId, // < trackId
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

        output.sampleData(audioChunk, audioChunk.capacity());
        output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, audioChunk.capacity(), 0, null);
	}

}
