package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.client.StreamBundle;

import java.util.List;

abstract class StreamReader {

    private final PacketQueue output;
    public final StreamBundle.Stream stream;
    private boolean hasFormat;

    protected StreamReader(PacketQueue output, StreamBundle.Stream stream) {
        this.output = output;
        this.stream = stream;
        this.hasFormat = false;
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

    protected void consume(byte[] p, int size, long timeUs, int flags) {
        output.sampleData(p, size, timeUs, flags);
    }

    protected boolean createFormat() {
        return createFormat(null);
    }

    protected boolean createFormat(List<byte[]> initializationData) {
        if(isAudio()) {
            format(MediaFormat.createAudioFormat(
                    Integer.toString(stream.physicalId),
                    stream.getMimeType(),
                    stream.bitRate,
                    MediaFormat.NO_VALUE,
                    C.UNKNOWN_TIME_US,
                    stream.channels,
                    stream.sampleRate,
                    null,
                    stream.language));
            return true;
        }

        if(isVideo()) {
            format(MediaFormat.createVideoFormat(
                    Integer.toString(stream.physicalId), // << trackId
                    stream.getMimeType(),
                    MediaFormat.NO_VALUE,
                    MediaFormat.NO_VALUE,
                    C.UNKNOWN_TIME_US       ,
                    stream.width,
                    stream.height,
                    initializationData,
                    MediaFormat.NO_VALUE,
                    (float)stream.pixelAspectRatio));
            return true;
        }

        return false;
    }

    protected boolean hasFormat() {
        return this.hasFormat;
    }

    protected void format(MediaFormat format) {
        output.format(format);
        this.hasFormat = true;
    }
}
