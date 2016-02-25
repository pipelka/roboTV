package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR ADTS (AAC) stream.
 */
final class AdtsReader extends StreamReader {

	public AdtsReader(PacketQueue output, StreamBundle.Stream stream) {
		super(output, stream);

        output.format(MediaFormat.createAudioFormat(
                Integer.toString(stream.physicalId), // < trackId
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
	public void consume(byte[] data, long pesTimeUs) {
        output.sampleData(data, data.length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
	}

}
