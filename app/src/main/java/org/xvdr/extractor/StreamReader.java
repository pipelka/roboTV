package org.xvdr.extractor;

import org.xvdr.robotv.client.StreamBundle;

abstract class StreamReader {

    protected final PacketQueue output;
    public final StreamBundle.Stream stream;

    protected StreamReader(PacketQueue output, StreamBundle.Stream stream) {
        this.output = output;
        this.stream = stream;
    }

    public boolean isVideo() {
        return (stream.content == StreamBundle.CONTENT_VIDEO);
    }

    public boolean isAudio() {
        return (stream.content == StreamBundle.CONTENT_AUDIO);
    }

    public abstract void consume(Allocation buffer);

}
