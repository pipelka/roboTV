package org.xvdr.extractor;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioDecoderException;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;

abstract class RoboTvAudioDecoder extends SimpleDecoder<DecoderInputBuffer, SimpleOutputBuffer, AudioDecoderException> {

    /**
     * @param inputBuffers  An array of nulls that will be used to store references to input buffers.
     * @param outputBuffers An array of nulls that will be used to store references to output buffers.
     */
    RoboTvAudioDecoder(DecoderInputBuffer[] inputBuffers, SimpleOutputBuffer[] outputBuffers) {
        super(inputBuffers, outputBuffers);
    }

    abstract protected AudioDecoderException decode(DecoderInputBuffer inputBuffer, SimpleOutputBuffer outputBuffer, boolean reset);

    abstract Format getOutputFormat();
}
