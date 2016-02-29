package org.xvdr.extractor;

import org.xvdr.robotv.client.StreamBundle;

abstract class StreamReader {

    protected final PacketQueue output;
    public final StreamBundle.Stream stream;

    protected StreamReader(PacketQueue output, StreamBundle.Stream stream) {
        this.output = output;
        this.stream = stream;
    }

    public abstract void consume(Allocation buffer, long pesTimeUs);

}
