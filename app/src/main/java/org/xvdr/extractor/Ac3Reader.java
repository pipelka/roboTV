package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.audio.AC3Decoder;
import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends StreamReader {

    private static final String TAG = "Ac3Reader";
    private boolean ac3PassThrough;
    boolean hasOutputFormat = false;

    AC3Decoder mDecoder;

    public Ac3Reader(DefaultTrackOutput output, StreamBundle.Stream stream) {
        this(output, stream, false);
    }

    public Ac3Reader(DefaultTrackOutput output, StreamBundle.Stream stream, boolean ac3PassThrough) {
        this(output, stream, ac3PassThrough, AC3Decoder.LayoutStereo);
    }

	public Ac3Reader(DefaultTrackOutput output, StreamBundle.Stream stream, boolean ac3PassThrough, int channelMode) {
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

        mDecoder = new AC3Decoder(channelMode);
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
        if(ac3PassThrough) {
            output.sampleData(data, data.capacity());
            output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, data.capacity(), 0, null);
            return;
        }

        int length = mDecoder.decode(data.data, 0, data.capacity());
        if(length == 0) {
            Log.e(TAG, "Unable to decode frame data");
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
                    mDecoder.getBitRate(),
                    length,
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
