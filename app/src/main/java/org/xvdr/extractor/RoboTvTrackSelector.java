package org.xvdr.extractor;

import android.os.Handler;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.MimeTypes;

class RoboTvTrackSelector extends DefaultTrackSelector {

    private String audioTrackId;

    RoboTvTrackSelector(Handler eventHandler) {
        super(eventHandler);
    }

    private static boolean isSupported(int formatSupport) {
        return (formatSupport & RendererCapabilities.FORMAT_SUPPORT_MASK) == RendererCapabilities.FORMAT_HANDLED;
    }

    @Override
    protected TrackSelection selectAudioTrack(TrackGroupArray groups, int[][] formatSupport, String preferredAudioLanguage) {
        TrackGroup selectedGroup = null;

        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;

        for(int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];

            for(int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {

                if(isSupported(trackFormatSupport[trackIndex])) {
                    Format format = trackGroup.getFormat(trackIndex);
                    boolean isDefault = (format.selectionFlags & C.SELECTION_FLAG_DEFAULT) != 0;
                    int trackScore;

                    if(format.id.equals(audioTrackId)) {
                        trackScore = 5;
                    }
                    else if (format.language.equals(preferredAudioLanguage)) {
                        if (format.sampleMimeType.equals(MimeTypes.AUDIO_AC3)) {
                            trackScore = 4;
                        }
                        else {
                            trackScore = 3;
                        }
                    }
                    else if (isDefault) {
                        trackScore = 2;
                    }
                    else {
                        trackScore = 1;
                    }

                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
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
