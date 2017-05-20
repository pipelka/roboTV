package org.xvdr.player;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;

public class RoboTvLoadControl implements LoadControl {

    private static final int MIN_BUFFER_LOAD = 3 * 1024 * 1024;

    DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);

    @Override
    public void onPrepared() {
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onStopped() {
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
        return allocator.getTotalBytesAllocated() > 0;
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs) {
        return allocator.getTotalBytesAllocated() < MIN_BUFFER_LOAD;
    }
}
