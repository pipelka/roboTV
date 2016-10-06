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
import com.google.android.exoplayer2.util.MimeTypes;

class RoboTvTrackSelector extends DefaultTrackSelector {

    private String audioTrackId;
    private String audioLanguage;

    RoboTvTrackSelector(Handler eventHandler) {
        super(eventHandler);
    }

    private static boolean isSupported(int formatSupport) {
        return (formatSupport & RendererCapabilities.FORMAT_SUPPORT_MASK) == RendererCapabilities.FORMAT_HANDLED;
    }

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports) throws ExoPlaybackException {
        TrackSelection[] trackSelection = super.selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);

        int[] trackScore = new int[trackSelection.length];

        int bestScore = -1;
        int bestRenderer = -1;

        // find the one and only audio renderer
        for(int i = 0; i < trackSelection.length; i++) {

            // skip non-audio / disabled tracks
            if(rendererCapabilities[i].getTrackType() != C.TRACK_TYPE_AUDIO || trackSelection[i] == null) {
                trackScore[i] = -1;
                continue;
            }

            int score = getScoreFromFormat(trackSelection[i].getSelectedFormat());
            trackScore[i] = score;

            if(score > bestScore) {
                bestScore = score;
                bestRenderer = i;
            }
        }

        // we didn't find the best renderer ?
        if(bestRenderer == -1)  {
            return trackSelection;
        }

        // enable just the one and only audio renderer
        for(int i = 0; i < trackSelection.length; i++) {

            // skip non-audio tracks
            if(rendererCapabilities[i].getTrackType() != C.TRACK_TYPE_AUDIO) {
                continue;
            }

            // disable selection for all other audio renderers
            if(i != bestRenderer) {
                trackSelection[i] = null;
            }
        }

        return trackSelection;
    }

    @Override
    protected TrackSelection selectAudioTrack(TrackGroupArray groups, int[][] formatSupport, String preferredAudioLanguage) {
        TrackGroup selectedGroup = null;
        audioLanguage = preferredAudioLanguage;

        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;

        for(int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];

            for(int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {

                if(isSupported(trackFormatSupport[trackIndex])) {
                    Format format = trackGroup.getFormat(trackIndex);
                    int trackScore = getScoreFromFormat(format);

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

    private int getScoreFromFormat(Format format) {
        if(format == null) {
            return -1;
        }

        int trackScore = 1;

        if(audioTrackId != null && format.id.equals(audioTrackId)) {
            trackScore = 5;
        }
        else {
            trackScore += (format.language.equals(audioLanguage) ? 2 : 0);
            trackScore += (format.sampleMimeType.equals(MimeTypes.AUDIO_AC3) ? 1 : 0);
        }

        return trackScore;
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
