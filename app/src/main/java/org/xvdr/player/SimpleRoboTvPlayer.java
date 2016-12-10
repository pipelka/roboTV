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
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import org.xvdr.player.audio.RoboTvAudioRenderer;
import org.xvdr.player.video.VideoRendererFactory;

import java.util.ArrayList;

public class SimpleRoboTvPlayer extends SimpleExoPlayer {

    private boolean audioPassthrough;
    private AudioCapabilities audioCapabilities;

    protected SimpleRoboTvPlayer(Context context, TrackSelector trackSelector, boolean audioPassthrough) {
        super(context, trackSelector, new RoboTvLoadControl(), null, EXTENSION_RENDERER_MODE_OFF, 5000);

        this.audioPassthrough = audioPassthrough;
        this.audioCapabilities = AudioCapabilities.getCapabilities(context);
    }

    protected void buildAudioRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       int extensionRendererMode, AudioRendererEventListener eventListener,
                                       ArrayList<Renderer> out) {

        // codecSelector disabling MPEG audio (handled by RoboTvAudioDecoder)
        MediaCodecSelector codecSelector = new MediaCodecSelector() {
            @Override
            public MediaCodecInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder, boolean requiresTunneling) throws MediaCodecUtil.DecoderQueryException {
                if (mimeType.equals(MimeTypes.AUDIO_MPEG)) {
                    return null;
                }

                if (mimeType.equals(MimeTypes.AUDIO_AC3) && !SimpleRoboTvPlayer.this.audioPassthrough) {
                    return null;
                }

                return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder, requiresTunneling);
            }

            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                return MediaCodecUtil.getPassthroughDecoderInfo();
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
