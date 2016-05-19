package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR H262 byte stream
 */
final class H262Reader extends StreamReader {

    private static final String TAG = "H262Reader";

    public H262Reader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);

        output.format(MediaFormat.createVideoFormat(
                          Integer.toString(stream.physicalId), // << trackId
                          MimeTypes.VIDEO_MPEG2,
                          MediaFormat.NO_VALUE,
                          MediaFormat.NO_VALUE,
                          C.UNKNOWN_TIME_US       ,
                          stream.width,
                          stream.height,
                          null,
                          MediaFormat.NO_VALUE,
                          stream.pixelAspectRatio));
    }

    @Override
    public void consume(Allocation buffer) {
        output.sampleData(buffer);
    }
}
