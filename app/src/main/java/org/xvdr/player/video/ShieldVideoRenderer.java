package org.xvdr.player.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class ShieldVideoRenderer extends MediaCodecVideoRenderer {

    private long startTime = 0;
    private boolean enabled = true;

    ShieldVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, int videoScalingMode, long allowedJoiningTimeMs, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, videoScalingMode, allowedJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected void onEnabled(boolean joining) throws ExoPlaybackException {
        startTime = System.currentTimeMillis();
        super.onEnabled(joining);
    }

    @Override
    protected void configureCodec(MediaCodec codec, Format format, MediaCrypto crypto) {
        super.configureCodec(codec, format, crypto);
        enabled = (format.height == 1080 || format.height == 576);
    }

    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        long currentTime = System.currentTimeMillis();

        if(enabled && currentTime - startTime > 13 * 60 * 1000) {
            releaseCodec();
            startTime = currentTime;
        }

        super.render(positionUs, elapsedRealtimeUs);
    }
}
