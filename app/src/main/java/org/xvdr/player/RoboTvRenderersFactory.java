package org.xvdr.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.audio.SonicAudioProcessor;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.audio.RoboTvAudioRenderer;
import org.xvdr.player.video.VideoRendererFactory;

import java.util.ArrayList;

public class RoboTvRenderersFactory implements RenderersFactory {

    private final boolean audioPassthrough;
    private final Context context;

    RoboTvRenderersFactory(Context context, boolean audioPassthrough) {
        this.context = context;
        this.audioPassthrough = audioPassthrough;
    }

    @Override
    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextRenderer.Output textRendererOutput, MetadataRenderer.Output metadataRendererOutput) {
        ArrayList<Renderer> out = new ArrayList<>();

        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        // codecSelector disabling MPEG audio (handled by RoboTvAudioDecoder)
        MediaCodecSelector codecSelector = new MediaCodecSelector() {
            @Override
            public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
                return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder);
            }

            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                return audioPassthrough ? MediaCodecUtil.getPassthroughDecoderInfo() : null;
            }
        };

        out.add(new RoboTvAudioRenderer(
                eventHandler,
                null,
                audioPassthrough,
                new SonicAudioProcessor())
        );

        out.add(new MediaCodecAudioRenderer(
                codecSelector,
                null,
                true,
                eventHandler,
                null,
                audioCapabilities,
                new SonicAudioProcessor())
        );

        out.add(VideoRendererFactory.create(context, eventHandler, videoRendererEventListener));

        return out.toArray(new Renderer[out.size()]);
    }
}
