package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import android.util.Log;

import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR H264 byte stream
 */
final class H264Reader extends ElementaryStreamReader {

	private static final String TAG = "H264Reader";

    private boolean hasOutputFormat;

	// Mediaformat
	private int frameWidth;
	private int frameHeight;
	private float pixelAspectRatio;

	public H264Reader(TrackOutput output, StreamBundle.Stream stream) {
        super(output);
		frameWidth = stream.width;
		frameHeight = stream.height;

		// XVDR sends the picture aspect ratio
		// we have to convert it to the pixel aspect ratio
        double value = (stream.aspect * frameHeight) / (double)frameWidth;
        pixelAspectRatio = (float)Math.round(value * 1000) / 1000;

	}

	@Override
	public void seek() {
	}

	@Override
    public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe, long durationUs) {
        if(!hasOutputFormat && isKeyframe) {
            output.format(MediaFormat.createVideoFormat(
                    MediaFormat.NO_VALUE, // << trackId
                    MimeTypes.VIDEO_H264,
                    MediaFormat.NO_VALUE,
                    MediaFormat.NO_VALUE,
                    durationUs,
                    frameWidth,
                    frameHeight,
                    null/*decoderInitData*/,
                    MediaFormat.NO_VALUE,
                    pixelAspectRatio));

            hasOutputFormat = true;
            Log.i(TAG, "MediaFormat: " + frameWidth + "x" + frameHeight + " aspect: " + pixelAspectRatio);
        }

        while(data.bytesLeft() > 0) {
            output.sampleData(data, data.bytesLeft());
        }

        int flags = isKeyframe ? C.SAMPLE_FLAG_SYNC : 0;
        output.sampleMetadata(pesTimeUs, flags, data.limit(), 0, null);
    }

	@Override
	public void packetFinished() {
	}

}
