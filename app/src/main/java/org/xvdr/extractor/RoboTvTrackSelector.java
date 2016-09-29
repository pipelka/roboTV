package org.xvdr.extractor;

import android.os.Handler;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;

class RoboTvTrackSelector extends DefaultTrackSelector {

    private String audioTrackId;

    RoboTvTrackSelector(Handler eventHandler) {
        super(eventHandler);
    }

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports) throws ExoPlaybackException {
        TrackSelection[] rendererTrackSelections = super.selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);

        if (audioTrackId == null || audioTrackId.isEmpty()) {
            return rendererTrackSelections;
        }

        for (int i = 0; i < rendererCapabilities.length; i++) {
            if(rendererCapabilities[i].getTrackType() == C.TRACK_TYPE_AUDIO) {
                TrackSelection selection = selectTrackById(rendererTrackGroupArrays[i], rendererFormatSupports[i], audioTrackId);
                if(selection != null) {
                    rendererTrackSelections[i] = selection;
                }
            }
        }

        return rendererTrackSelections;
    }

    private TrackSelection selectTrackById(TrackGroupArray rendererTrackGroupArray, int[][] formatSupport, String audioTrackId) {
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;

        // iterate track group array
        for(int groupIndex = 0; groupIndex < rendererTrackGroupArray.length && selectedGroup == null; groupIndex++) {

            TrackGroup trackGroup = rendererTrackGroupArray.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];

            // iterate track group
            for(int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {

                // skip unhandled formats
                if((trackFormatSupport[trackIndex] & RendererCapabilities.FORMAT_SUPPORT_MASK) != RendererCapabilities.FORMAT_HANDLED) {
                    continue;
                }

                Format format = trackGroup.getFormat(trackIndex);

                // select track with matching id
                if(format.id.equals(audioTrackId)) {
                    selectedGroup = trackGroup;
                    selectedTrackIndex = trackIndex;
                    break;
                }
            }
        }

        return selectedGroup == null ? null : new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    void selectAudioTrack(String trackId) {
        audioTrackId = trackId;
        invalidate();
    }

    void clearAudioTrack() {
        audioTrackId = null;
        invalidate();
    }
}
