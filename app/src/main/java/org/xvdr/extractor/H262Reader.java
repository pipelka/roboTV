package org.xvdr.extractor;

import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR H262 byte stream
 */
final class H262Reader extends StreamReader {

    private static final String TAG = "H262Reader";

    public H262Reader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);
        createFormat();
    }

}
