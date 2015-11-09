package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR H264 byte stream
 */
final class H264Reader extends StreamReader {

	private static final String TAG = "H264Reader";

	public H264Reader(DefaultTrackOutput output, StreamBundle.Stream stream) {
        super(output, stream);

        // XVDR sends the picture aspect ratio
        // we have to convert it to the pixel aspect ratio
        double value = (stream.aspect * stream.height) / (double)stream.width;
        float pixelAspectRatio = (float)Math.round(value * 1000) / 1000;

        output.format(MediaFormat.createVideoFormat(
                stream.physicalId, // << trackId
                MimeTypes.VIDEO_H264,
                MediaFormat.NO_VALUE,
                MediaFormat.NO_VALUE,
                C.UNKNOWN_TIME_US,
                stream.width,
                stream.height,
                null/*decoderInitData*/,
                MediaFormat.NO_VALUE,
                pixelAspectRatio));
	}

	@Override
    public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
        int flags = isKeyframe ? C.SAMPLE_FLAG_SYNC : 0;

        output.sampleData(data, data.capacity());
        output.sampleMetadata(pesTimeUs, flags, data.capacity(), 0, null);
    }

}
