package org.xvdr.extractor;

import org.xvdr.robotv.client.StreamBundle;

/**
 * Processes a XVDR ADTS (AAC) stream.
 */
final class AdtsReader extends StreamReader {

    public AdtsReader(PacketQueue output, StreamBundle.Stream stream) {
        super(output, stream);
        createFormat();
    }

}
