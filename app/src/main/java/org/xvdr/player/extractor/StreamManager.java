package org.xvdr.player.extractor;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

class StreamManager extends SparseArray<StreamReader> {

    private static final String TAG = "StreamManager";

    private static final int MAX_OUTPUT_TRACKS = 8;

    private final TrackOutput[] trackOutput;

    private static final String MIMETYPE_UNKNOWN = "unknown/unknown";

    StreamManager() {
        trackOutput = new TrackOutput[MAX_OUTPUT_TRACKS];
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
            trackOutput[i] = output.track(i, C.TRACK_TYPE_DEFAULT);
        }

        updateStreams(bundle);

        output.endTracks();
    }

    void updateStreams(StreamBundle bundle) {
        int index = 0;
        clear();

        // create stream readers
        for(StreamBundle.Stream stream : bundle) {
            int pid = stream.physicalId;

            if(isSteamSupported(stream)) {
                put(pid, new StreamReader(trackOutput[index++], stream));
            }

            if(index >= MAX_OUTPUT_TRACKS) {
                Log.e(TAG, "maximum stream count of " + MAX_OUTPUT_TRACKS + " reached. skipping other streams");
                return;
            }
        }

        // fill remaining tracks
        for(int i = index; i < MAX_OUTPUT_TRACKS; i++) {
            Format format = Format.createContainerFormat(null, MIMETYPE_UNKNOWN, MIMETYPE_UNKNOWN,
                            null, Format.NO_VALUE, 0, null);
            trackOutput[i].format(format);
        }
    }
}
