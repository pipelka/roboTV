package org.robotv.player.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

class ShieldVideoRenderer extends MediaCodecVideoRenderer {

    final static private long resetDuration = 13 * 60 * 1000;

    private long startTime;
    private long counter = 0;
    private long lastKeyFrame = 0;
    private boolean enabled;
    private boolean resetCodec = false;
    private long gopSize = 0;

    ShieldVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format, MediaCrypto crypto)  throws MediaCodecUtil.DecoderQueryException {
        super.configureCodec(codecInfo, codec, format, crypto);

        enabled = (format.height == 1080 || format.height == 576);
        startTime = System.currentTimeMillis();
        resetCodec = false;
    }

    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if(enabled && resetCodec) {
            releaseCodec();
        }

        super.render(positionUs, elapsedRealtimeUs);
    }

    @Override
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {
        long currentTime = System.currentTimeMillis();
        long diffMs = currentTime - startTime;

        counter++;

        if(buffer.isKeyFrame()) {
            gopSize = (lastKeyFrame > 0) ? (counter - lastKeyFrame) : counter;
            lastKeyFrame = counter;
        }

        long gopFrame = counter - lastKeyFrame;

        if(enabled && gopFrame == (gopSize - 1) && diffMs > resetDuration) {
            resetCodec = true;
        }

        super.onQueueInputBuffer(buffer);
    }

}
