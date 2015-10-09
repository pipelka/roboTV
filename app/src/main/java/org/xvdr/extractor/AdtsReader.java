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

    private int sampleRate;

    private int channels;

	public AdtsReader(TrackOutput output, StreamBundle.Stream stream) {
		super(output);
        sampleRate = stream.sampleRate;
        channels = stream.channels;
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
                    MediaFormat.NO_VALUE, // << bitrate
                    MediaFormat.NO_VALUE,
                    MediaFormat.NO_VALUE, // < duration
                    channels,
                    sampleRate,
                    null,
                    null)); // < language
            hasOutputFormat = true;
        }

        output.sampleData(data, data.capacity());
        output.sampleMetadata(pesTimeUs, C.SAMPLE_FLAG_SYNC, data.capacity(), 0, null);
	}

	@Override
	public void packetFinished() {
	}

}
