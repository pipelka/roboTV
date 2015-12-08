package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes a XVDR H264 byte stream
 */
final class H264Reader extends StreamReader {

	private static final String TAG = "H264Reader";

	public H264Reader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);

        // XVDR sends the picture aspect ratio
        // we have to convert it to the pixel aspect ratio
        double value = (stream.aspect * stream.height) / (double)stream.width;
        float pixelAspectRatio = (float)Math.round(value * 1000) / 1000;

        // do we have the decoder init data ?
        List<byte[]> initializationData = null;

        if(stream.spsLength != 0 && stream.ppsLength != 0) {
            initializationData = new ArrayList<>();
            assembleInitData(initializationData);
        }

        output.format(MediaFormat.createVideoFormat(
                Integer.toString(stream.physicalId), // << trackId
                MimeTypes.VIDEO_H264,
                MediaFormat.NO_VALUE,
                256 * 1024,
                C.UNKNOWN_TIME_US       ,
                stream.width,
                stream.height,
                initializationData,
                MediaFormat.NO_VALUE,
                pixelAspectRatio));
	}

	@Override
    public void consume(byte[] data, long pesTimeUs, boolean isKeyframe) {
        int flags = isKeyframe ? C.SAMPLE_FLAG_SYNC : 0;

        output.sampleData(data, data.length, pesTimeUs, flags);
    }

    private void assembleInitData(List<byte[]> data) {
        byte[] initSps = new byte[stream.spsLength + 4];
        byte[] initPps = new byte[stream.ppsLength + 4];

        // http://developer.android.com/reference/android/media/MediaCodec.html

        // add header 0x00 0x00 0x00 0x01
        initSps[3] = 0x01;
        System.arraycopy(stream.sps, 0, initSps, 4, stream.spsLength);
        data.add(initSps);

        // add header 0x00 0x00 0x00 0x01
        initPps[3] = 0x01;
        System.arraycopy(stream.pps, 0, initPps, 4, stream.ppsLength);
        data.add(initPps);
    }
}
