package org.xvdr.player.video;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class ShieldVideoRenderer extends MediaCodecVideoRenderer {

    private long startTime = 0;

    public ShieldVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, int videoScalingMode, long allowedJoiningTimeMs, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, videoScalingMode, allowedJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected void onEnabled(boolean joining) throws ExoPlaybackException {
        startTime = System.currentTimeMillis();
        super.onEnabled(joining);
    }

    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        long currentTime = System.currentTimeMillis();

        if(currentTime - startTime > 13 * 60 * 1000) {
            releaseCodec();
            startTime = currentTime;
        }

        super.render(positionUs, elapsedRealtimeUs);
    }
}
