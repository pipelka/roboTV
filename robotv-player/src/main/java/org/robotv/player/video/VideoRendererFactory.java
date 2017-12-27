package org.robotv.player.video;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class VideoRendererFactory {

    static public Renderer create(Context context, Handler handler, VideoRendererEventListener listener) {

        if(Build.MODEL.equals("SHIELD Android TV")) {
            return new ShieldVideoRenderer(
                    context,
                    MediaCodecSelector.DEFAULT,
                    5000, // joining time
                    null,
                    true,
                    handler,
                    listener,
                    50);
        }

        return new MediaCodecVideoRenderer(
                context,
                MediaCodecSelector.DEFAULT,
                5000, // joining time
                null,
                true,
                handler,
                listener,
                50);
    }
}
