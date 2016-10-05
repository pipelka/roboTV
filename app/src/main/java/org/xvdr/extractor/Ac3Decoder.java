package org.xvdr.extractor;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioDecoderException;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.util.MimeTypes;

import java.nio.ByteBuffer;

class Ac3Decoder extends RoboTvAudioDecoder {

    final static int AC3_LAYOUT_STEREO = 2;
    final static int AC3_LAYOUT_DOLBY = 10;
    final static int AC3_LAYOUT_SURROUND51 = 21; // 3F2R + LFE

    private final long context;

    private native long init(int flags);

    private native void release(long context);

    private native int decode(long context, ByteBuffer input, int inputLength, ByteBuffer output);

    private native int getChannelCount(long context);

    private native int getSampleRate(long context);

    /**
     * @param inputBuffers  An array of nulls that will be used to store references to input buffers.
     * @param outputBuffers An array of nulls that will be used to store references to output buffers.
     */
    Ac3Decoder(DecoderInputBuffer[] inputBuffers, SimpleOutputBuffer[] outputBuffers, int layout) {
        super(inputBuffers, outputBuffers);
        setInitialInputBufferSize(4096);
        context = init(layout);
    }

    @Override
    protected DecoderInputBuffer createInputBuffer() {
        return new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
    }

    @Override
    protected SimpleOutputBuffer createOutputBuffer() {
        return new SimpleOutputBuffer(this);
    }

    @Override
    protected AudioDecoderException decode(DecoderInputBuffer inputBuffer, SimpleOutputBuffer outputBuffer, boolean reset) {
        // max. ac3 buffer size = sizeof(int) * channels * samplesPerBlock * blocks
        // 2 * 6 * 256 * 6 = 18432
        ByteBuffer outputData = outputBuffer.init(inputBuffer.timeUs, 18432);

        int size = decode(context, inputBuffer.data, inputBuffer.data.limit(), outputData);

        outputData.position(0);
        outputData.limit(size);

        return null;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void release() {
        super.release();
        release(context);
    }

    @Override
    Format getOutputFormat() {
        int channelCount = getChannelCount(context);
        int sampleRate = getSampleRate(context);

        return Format.createAudioSampleFormat(null, MimeTypes.AUDIO_RAW, null, Format.NO_VALUE,
                Format.NO_VALUE, channelCount, sampleRate, C.ENCODING_PCM_16BIT, null, null, 0, null);
    }

}
