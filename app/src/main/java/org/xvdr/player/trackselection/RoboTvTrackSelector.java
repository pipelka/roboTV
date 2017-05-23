package org.xvdr.player.trackselection;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;

public class RoboTvTrackSelector extends DefaultTrackSelector {

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports) throws ExoPlaybackException {
        TrackSelection[] trackSelection = super.selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);

        // find the one and only audio renderer
        for(int i = 0; i < trackSelection.length; i++) {

            // skip non-audio / disabled tracks
            if(rendererCapabilities[i].getTrackType() != C.TRACK_TYPE_AUDIO || trackSelection[i] == null) {
                continue;
            }

            int s = rendererCapabilities[i].supportsFormat(trackSelection[i].getSelectedFormat());
            if((s & RendererCapabilities.FORMAT_SUPPORT_MASK) == 0)  {
                trackSelection[i] = null;
            }
        }

        return trackSelection;
    }
}
