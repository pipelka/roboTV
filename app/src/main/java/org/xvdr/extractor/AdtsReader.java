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
final class AdtsReader extends ElementaryStreamReader {

	private boolean hasOutputFormat;

    private StreamBundle.Stream mStream;

	public AdtsReader(TrackOutput output, StreamBundle.Stream stream) {
		super(output);
        mStream = stream;
	}

	@Override
	public void seek() {
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe, long durationUs) {
        if(!hasOutputFormat) {
            output.format(MediaFormat.createAudioFormat(
                    MediaFormat.NO_VALUE, // < trackId
                    MimeTypes.AUDIO_AAC,
                    mStream.bitRate,
                    MediaFormat.NO_VALUE,
                    MediaFormat.NO_VALUE, // < duration
                    mStream.channels,
                    mStream.sampleRate,
                    null,
                    mStream.language));
            hasOutputFormat = true;
        }

        output.sampleData(data, data.capacity());
        output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, data.capacity(), 0, null);
	}

	@Override
	public void packetFinished() {
	}

}
