package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.robotv.tv.StreamBundle;

import java.util.Collections;
import java.util.List;

/**
 * Processes a XVDR H265 byte stream
 */
final class H265Reader extends StreamReader {

	private static final String TAG = "H265Reader";

	public H265Reader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);

        // do we have the decoder init data ?
        List<byte[]> initializationData = null;

        if(stream.spsLength != 0 && stream.ppsLength != 0 && stream.vpsLength != 0) {
            initializationData = assembleInitData();
        }

        output.format(MediaFormat.createVideoFormat(
                Integer.toString(stream.physicalId), // << trackId
                MimeTypes.VIDEO_H265,
                MediaFormat.NO_VALUE,
                MediaFormat.NO_VALUE,
                C.UNKNOWN_TIME_US,
                stream.width,
                stream.height,
                initializationData,
                MediaFormat.NO_VALUE,
                stream.pixelAspectRatio));
	}

	@Override
    public void consume(byte[] data, long pesTimeUs) {
        output.sampleData(data, data.length, pesTimeUs, C.SAMPLE_FLAG_SYNC);
    }

    private List<byte[]> assembleInitData() {
        byte[] sps = new byte[stream.spsLength + 4];
        byte[] pps = new byte[stream.ppsLength + 4];
        byte[] vps = new byte[stream.vpsLength + 4];

        // http://developer.android.com/reference/android/media/MediaCodec.html

        // add header 0x00 0x00 0x00 0x01
        sps[3] = 0x01;
        System.arraycopy(stream.sps, 0, sps, 4, stream.spsLength);

        // add header 0x00 0x00 0x00 0x01
        pps[3] = 0x01;
        System.arraycopy(stream.pps, 0, pps, 4, stream.ppsLength);

        // add header 0x00 0x00 0x00 0x01
        vps[3] = 0x01;
        System.arraycopy(stream.vps, 0, vps, 4, stream.vpsLength);

        byte[] csd = new byte[vps.length + sps.length + pps.length];
        System.arraycopy(vps, 0, csd, 0, vps.length);
        System.arraycopy(sps, 0, csd, vps.length, sps.length);
        System.arraycopy(pps, 0, csd, vps.length + sps.length, pps.length);

        return Collections.singletonList(csd);
    }
}
