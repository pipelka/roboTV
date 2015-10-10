package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.Ac3Util;
import com.google.android.exoplayer.util.ParsableBitArray;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR AC3 stream.
 */
final class Ac3Reader extends ElementaryStreamReader {

    private static final String TAG = "Ac3Reader";

    private boolean hasOutputFormat = false;
    private StreamBundle.Stream mStream;

	public Ac3Reader(TrackOutput output, StreamBundle.Stream stream) {
		super(output);
        mStream = stream;
	}

	@Override
	public void seek() {
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe, long durationUs) {
        if(!hasOutputFormat) {
            output.format(Ac3Util.parseFrameAc3Format(
                    new ParsableBitArray(data.data),
                    mStream.physicalId,
                    C.UNKNOWN_TIME_US,
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
