package org.xvdr.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.audio.RoboTvAudioRenderer;
import org.xvdr.player.video.VideoRendererFactory;

import java.util.ArrayList;

class SimpleRoboTvPlayer extends SimpleExoPlayer {

    SimpleRoboTvPlayer(Context context, TrackSelector trackSelector, boolean audioPassthrough) {
        // this is the ugliest hack on the planet ,...
        // since we need to set audioPassthrough before super() (which isn't possible)
        // we pass EXTENSION_RENDERER_MODE_OFF / EXTENSION_RENDERER_MODE_ON for false / true
        super(context, trackSelector, new RoboTvLoadControl(), null,
                audioPassthrough ? EXTENSION_RENDERER_MODE_ON : EXTENSION_RENDERER_MODE_OFF, 5000);
    }

    protected void buildAudioRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       int extensionRendererMode, AudioRendererEventListener eventListener,
                                       ArrayList<Renderer> out) {

        final boolean audioPassthrough = (extensionRendererMode == EXTENSION_RENDERER_MODE_ON);
        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);

        // codecSelector disabling MPEG audio (handled by RoboTvAudioDecoder)
        MediaCodecSelector codecSelector = new MediaCodecSelector() {
            @Override
            public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder, boolean requiresTunneling) throws MediaCodecUtil.DecoderQueryException {
                return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder, requiresTunneling);
            }

            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                return audioPassthrough ? MediaCodecUtil.getPassthroughDecoderInfo() : null;
            }
        };

        out.add(new RoboTvAudioRenderer(
                mainHandler,
                null,
                audioPassthrough)
        );

        out.add(new MediaCodecAudioRenderer(
                codecSelector,
                null,
                true,
                mainHandler,
                null,
                audioCapabilities)
        );
    }

    protected void buildVideoRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       int extensionRendererMode, VideoRendererEventListener eventListener,
                                       long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        out.add(VideoRendererFactory.create(context, mainHandler, eventListener));
    }
}
