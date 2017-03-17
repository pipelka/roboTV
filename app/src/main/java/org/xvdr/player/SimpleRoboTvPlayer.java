package org.xvdr.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.audio.RoboTvAudioRenderer;
import org.xvdr.player.video.VideoRendererFactory;

import java.util.ArrayList;

class SimpleRoboTvPlayer extends SimpleExoPlayer {

    private static final int DEFAULT_MIN_BUFFER_MS = 4000;
    private static final int DEFAULT_MAX_BUFFER_MS = 6000;
    private static final int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 1500;
    private static final int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS  = 2000;

    SimpleRoboTvPlayer(Context context, TrackSelector trackSelector, boolean audioPassthrough) {
        super(
                context,
                trackSelector,
                new DefaultLoadControl(
                        new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                        DEFAULT_MIN_BUFFER_MS,
                        DEFAULT_MAX_BUFFER_MS,
                        DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS),
                null,
                // this is the ugliest hack on the planet ,...
                // since we need to set audioPassthrough before super() (which isn't possible)
                // we pass EXTENSION_RENDERER_MODE_OFF / EXTENSION_RENDERER_MODE_ON for false / true
                audioPassthrough ? EXTENSION_RENDERER_MODE_ON : EXTENSION_RENDERER_MODE_OFF,
                5000);
    }

    @Override
    protected void buildAudioRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       @ExtensionRendererMode int extensionRendererMode, AudioRendererEventListener eventListener,
                                       AudioProcessor[] audioProcessors, ArrayList<Renderer> out) {
        final boolean audioPassthrough = (extensionRendererMode == EXTENSION_RENDERER_MODE_ON);
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
                mainHandler,
                null,
                audioPassthrough,
                audioProcessors)
        );

        out.add(new MediaCodecAudioRenderer(
                codecSelector,
                null,
                true,
                mainHandler,
                null,
                audioCapabilities,
                audioProcessors)
        );
    }

    @Override
    protected void buildVideoRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       int extensionRendererMode, VideoRendererEventListener eventListener,
                                       long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        out.add(VideoRendererFactory.create(context, mainHandler, eventListener));
    }
}
