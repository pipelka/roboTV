package org.xvdr.player.extractor;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.DefaultTrackOutput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

class StreamManager extends SparseArray<StreamReader> {

    private static final String TAG = "StreamManager";

    private static final int MAX_OUTPUT_TRACKS = 8;

    private static final String MIMETYPE_UNKNOWN = "unknown/unknown";

    StreamManager() {
    }

    private boolean isSteamSupported(StreamBundle.Stream stream) {
        switch(stream.getMimeType()) {
            case MimeTypes.VIDEO_MPEG2:
            case MimeTypes.VIDEO_H264:
            case MimeTypes.VIDEO_H265:
            case MimeTypes.AUDIO_AC3:
            case MimeTypes.AUDIO_E_AC3:
            case MimeTypes.AUDIO_MPEG:
            case MimeTypes.BASE_TYPE_AUDIO + "/aac_latm":
                return true;
        }

        return false;
    }

    void createStreams(ExtractorOutput output, StreamBundle bundle) {
        // pre-create output tracks
        for(int i = 0; i < MAX_OUTPUT_TRACKS; i++) {
            output.track(i, C.TRACK_TYPE_DEFAULT);
        }

        updateStreams(output, bundle);

        output.endTracks();
    }

    void updateStreams(ExtractorOutput output, StreamBundle bundle) {
        int index = 0;
        clear();

        // create stream readers
        for(StreamBundle.Stream stream : bundle) {
            int pid = stream.physicalId;

            if(isSteamSupported(stream)) {
                DefaultTrackOutput track = (DefaultTrackOutput) output.track(index++, C.TRACK_TYPE_DEFAULT);
                track.reset(true);
                put(pid, new StreamReader(track, stream));
            }

            if(index >= MAX_OUTPUT_TRACKS) {
                Log.e(TAG, "maximum stream count of " + MAX_OUTPUT_TRACKS + " reached. skipping other streams");
                return;
            }
        }

        // disable remaining tracks
        for(int i = index; i < MAX_OUTPUT_TRACKS; i++) {
            DefaultTrackOutput track = (DefaultTrackOutput) output.track(i, C.TRACK_TYPE_DEFAULT);
            Format format = Format.createSampleFormat(null, MIMETYPE_UNKNOWN, null, Format.NO_VALUE, null);
            track.format(format);
            track.reset(false);
        }
    }
}
