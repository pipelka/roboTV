package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;

public class RoboTvLoadControl  implements LoadControl {

    private static final String TAG = "RoboTvLoadControl";

    private final DefaultAllocator allocator;

    public RoboTvLoadControl() {
        allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
    }

    @Override
    public void onPrepared() {
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelections<?> trackSelections) {
    }

    @Override
    public void onStopped() {
        allocator.reset();
    }

    @Override
    public void onReleased() {
        allocator.reset();
    }

    @Override
    public Allocator getAllocator() {
        return allocator;
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, boolean rebuffering) {
        return rebuffering ? (bufferedDurationUs >= 2000) : (bufferedDurationUs > 0);
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs) {
        // WORKAROUND: sometimes we get insane high values here
        // it's currently unknown if it's a ExoPlayer2 or roboTV
        // issue.
        // When this happens ExoPlayer's buffers run dry because
        // it thinks there is plenty of data available (but it's
        // not). The effect is that playback simply stops.
        if(bufferedDurationUs > 1000 * 1000 * 60 * 60) {
            return true;
        }

        // we buffer up to 5 seconds
        return (bufferedDurationUs < 5000000);
    }
}
