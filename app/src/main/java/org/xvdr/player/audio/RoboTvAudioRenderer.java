package org.xvdr.player.audio;

import android.os.Handler;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.util.MimeTypes;

public class RoboTvAudioRenderer extends FfmpegAudioRenderer {

    private boolean ac3Passthrough;

    public RoboTvAudioRenderer(Handler eventHandler, AudioRendererEventListener eventListener, boolean ac3Passthrough) {
        super(eventHandler, eventListener);
        this.ac3Passthrough = ac3Passthrough;
    }

    @Override
    public int supportsFormat(Format format) {
        if(format.sampleMimeType.equals(MimeTypes.AUDIO_E_AC3)) {
            return FORMAT_HANDLED;
        }

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
}
