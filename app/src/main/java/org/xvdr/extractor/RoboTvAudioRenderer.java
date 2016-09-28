package org.xvdr.extractor;

import android.media.AudioManager;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioDecoderException;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.SimpleDecoderAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.util.MimeTypes;

class RoboTvAudioRenderer extends SimpleDecoderAudioRenderer {

    private static final int NUM_BUFFERS = 16;

    private RoboTvAudioDecoder decoder;

    RoboTvAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener) {
        super(eventHandler, eventListener, null, AudioManager.STREAM_MUSIC);
    }

    RoboTvAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities, int streamType) {
        super(eventHandler, eventListener, audioCapabilities, streamType);
    }

    @Override
    public int supportsFormat(Format format) throws ExoPlaybackException {
        if(format.sampleMimeType.equals(MimeTypes.AUDIO_AC3)){
            return FORMAT_HANDLED;
        }

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_MPEG)){
            return FORMAT_HANDLED;
        }

        if(MimeTypes.isAudio(format.sampleMimeType)) {
            return FORMAT_UNSUPPORTED_SUBTYPE;
        }

        return FORMAT_UNSUPPORTED_TYPE;
    }

    @Override
    protected RoboTvAudioDecoder createDecoder(Format format) throws AudioDecoderException {
        decoder = null;

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_AC3)){
            decoder = new Ac3Decoder(new DecoderInputBuffer[NUM_BUFFERS], new SimpleOutputBuffer[NUM_BUFFERS], format);
        }

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_MPEG)){
            decoder = new MpegAudioDecoder(new DecoderInputBuffer[NUM_BUFFERS], new SimpleOutputBuffer[NUM_BUFFERS], format);
        }

        return decoder;
    }

    @Override
    public Format getOutputFormat() {
        return decoder.getOutputFormat();
    }

}
