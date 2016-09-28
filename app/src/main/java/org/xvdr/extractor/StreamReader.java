package org.xvdr.extractor;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.TrackOutput;

import org.xvdr.robotv.client.StreamBundle;

import java.io.IOException;
import java.util.List;

class StreamReader {

    private static final String TAG = "StreamReader";

    private final TrackOutput output;
    public final StreamBundle.Stream stream;

    StreamReader(TrackOutput output, StreamBundle.Stream stream) {
        this.output = output;
        this.stream = stream;

        createFormat();
    }

    public TrackOutput output() {
        return output;
    }

    public boolean isVideo() {
        return (stream.content == StreamBundle.CONTENT_VIDEO);
    }

    public boolean isAudio() {
        return (stream.content == StreamBundle.CONTENT_AUDIO);
    }

    protected void consume(ExtractorInput input, int size, long timeUs, int flags) throws IOException, InterruptedException {
        int length = size;

        while(length > 0) {
            length -= output.sampleData(input, length, false);
        }

        output.sampleMetadata(timeUs, flags, size, 0, null);
    }

    private boolean createFormat() {
        return createFormat(null);
    }

    private boolean createFormat(List<byte[]> initializationData) {
        String mimeType = stream.getMimeType();

        if(isAudio()) {
            output.format(Format.createAudioSampleFormat(
                    Integer.toString(stream.physicalId),
                    mimeType,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    stream.channels,
                    stream.sampleRate,
                    C.ENCODING_PCM_16BIT,
                    null, null,
                    0,
                    stream.language));
            return true;
        }

        if(isVideo()) {
            output.format(Format.createVideoSampleFormat(
                    Integer.toString(stream.physicalId), // << trackId
                    mimeType,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    stream.width,
                    stream.height,
                    stream.getFrameRate(),
                    initializationData,
                    0,
                    (float)stream.pixelAspectRatio,
                    null));
            return true;
        }

        return false;
    }
}
