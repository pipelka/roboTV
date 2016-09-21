package org.xvdr.extractor;

import com.google.android.exoplayer.SampleHolder;

import org.xvdr.jniwrap.Packet;
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

    public void consume(Packet p, int size, long timeUs, int flags) {
        output.sampleData(p, size, timeUs, flags);
    }

}
