package org.xvdr.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.video.VideoRendererFactory;

import java.util.ArrayList;

class RoboTvRenderersFactory implements RenderersFactory {

    private final boolean audioPassthrough;
    private final Context context;

    RoboTvRenderersFactory(Context context, boolean audioPassthrough) {
        this.context = context;
        this.audioPassthrough = audioPassthrough;
    }

    @Override
    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextOutput textRendererOutput, MetadataOutput metadataRendererOutput) {
        ArrayList<Renderer> out = new ArrayList<>();

        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        if(audioPassthrough) {
            out.add(new MediaCodecAudioRenderer(
                    new MediaCodecSelector() {
                        @Override
                        public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
                            return null;
                        }

                        @Override
                        public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                            return MediaCodecUtil.getPassthroughDecoderInfo();
                        }
                    },
                    null,
                    true,
                    eventHandler,
                    null,
                    audioCapabilities)
            );
        }

        out.add(new FfmpegAudioRenderer(
                eventHandler,
                null)
        );

        out.add(VideoRendererFactory.create(context, eventHandler, videoRendererEventListener));

        return out.toArray(new Renderer[out.size()]);
    }
}
