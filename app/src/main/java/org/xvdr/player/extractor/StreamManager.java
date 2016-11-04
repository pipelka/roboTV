package org.xvdr.player.extractor;

import android.util.SparseArray;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

class StreamManager extends SparseArray<StreamReader> {

    private static final String TAG = "StreamManager";

    private static final int MAX_OUTPUT_TRACKS = 8;

    private final TrackOutput[] trackOutput;

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
                return true;
        }

        return false;
    }

    void createStreams(ExtractorOutput output, StreamBundle bundle) {
        // pre-create output tracks
        for(int i = 0; i < MAX_OUTPUT_TRACKS; i++) {
            trackOutput[i] = output.track(i);
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
        }

        // fill remaining tracks
        for(int i = index; i < MAX_OUTPUT_TRACKS; i++) {
            Format format = Format.createContainerFormat("", null, null,
                            MimeTypes.VIDEO_UNKNOWN, Format.NO_VALUE);
            trackOutput[i].format(format);
        }
    }
}
