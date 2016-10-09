package org.xvdr.extractor;

import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioDecoderException;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.SimpleDecoderAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.util.MimeTypes;

class RoboTvAudioRenderer extends SimpleDecoderAudioRenderer {

    private static final String TAG = "RoboTvAudioRenderer";

    private static final int NUM_BUFFERS = 2;

    private RoboTvAudioDecoder decoder;
    private boolean ac3Passthrough;
    private int ac3Layout = Ac3Decoder.AC3_LAYOUT_SURROUND51;

    RoboTvAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, boolean ac3Passthrough, int channelConfiguration) {
        super(eventHandler, eventListener);
        this.ac3Passthrough = ac3Passthrough;

        switch(channelConfiguration) {
            case Player.CHANNELS_DIGITAL51:
                ac3Layout = Ac3Decoder.AC3_LAYOUT_SURROUND51;
                break;

            case Player.CHANNELS_STEREO:
                ac3Layout = Ac3Decoder.AC3_LAYOUT_STEREO;
                break;

            case Player.CHANNELS_SURROUND:
                ac3Layout = Ac3Decoder.AC3_LAYOUT_DOLBY;
                break;
        }
    }

    @Override
    public int supportsFormat(Format format) throws ExoPlaybackException {
        if(format.sampleMimeType.equals(MimeTypes.AUDIO_AC3) && !ac3Passthrough) {
            return FORMAT_HANDLED;
        }

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_MPEG)) {
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

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_AC3)) {
            decoder = new Ac3Decoder(new DecoderInputBuffer[NUM_BUFFERS], new SimpleOutputBuffer[NUM_BUFFERS], ac3Layout);
        }

        if(format.sampleMimeType.equals(MimeTypes.AUDIO_MPEG)) {
            decoder = new MpegAudioDecoder(new DecoderInputBuffer[NUM_BUFFERS], new SimpleOutputBuffer[NUM_BUFFERS], format);
        }

        return decoder;
    }

    @Override
    public Format getOutputFormat() {
        Format format = decoder.getOutputFormat();

        Log.d(TAG, "output format:");
        Log.d(TAG, "Channels: " + format.channelCount);
        Log.d(TAG, "Samplerate: " + format.sampleRate + " Hz");

        return format;
    }

}
