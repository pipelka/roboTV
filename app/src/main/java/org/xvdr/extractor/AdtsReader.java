package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR ADTS (AAC) stream.
 */
final class AdtsReader extends StreamReader {

	public AdtsReader(TrackOutput output, StreamBundle.Stream stream) {
		super(output, stream);

        output.format(MediaFormat.createAudioFormat(
                MediaFormat.NO_VALUE, // < trackId
                MimeTypes.AUDIO_AAC,
                stream.bitRate,
                MediaFormat.NO_VALUE,
                C.UNKNOWN_TIME_US, // < duration
                stream.channels,
                stream.sampleRate,
                null,
                stream.language));
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
        output.sampleData(data, data.capacity());
        output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, data.capacity(), 0, null);
	}

}
