package org.xvdr.extractor;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

class StreamManager extends SparseArray<StreamReader> {

    private static final String TAG = "StreamManager";

    private boolean isSteamSupported(StreamBundle.Stream stream) {
        switch (stream.getMimeType()) {
            case MimeTypes.VIDEO_MPEG2:
            case MimeTypes.VIDEO_H264:
            case MimeTypes.VIDEO_H265:
            case MimeTypes.AUDIO_AC3:
            case MimeTypes.AUDIO_MPEG:
                return true;
        }

        return false;
    }

    void createStreams(ExtractorOutput output, StreamBundle bundle) {
        for(StreamBundle.Stream stream: bundle) {
            int pid = stream.physicalId;

            if(isSteamSupported(stream)) {
                put(pid, new StreamReader(output.track(pid), stream));
            }
        }

        output.endTracks();
    }

    void updateStreams(StreamBundle bundle) {
        for(StreamBundle.Stream stream: bundle) {
            int pid = stream.physicalId;

            // skip unsupported stream types
            if(!isSteamSupported(stream)) {
                continue;
            }

            // check if we already have a stream of that pid and replace the old stream
            StreamReader currentReader = get(pid);
            if(currentReader != null) {
                Log.i(TAG, "updated stream with pid " + pid);
                put(pid, new StreamReader(currentReader.output(), stream));
                continue;
            }

            // there's a new pid in the stream. try to find a reader (pid) that is currently
            // unused (managed by StreamManager but not present in the StreamBundle)
            StreamReader maybeUnusedReader = null;
            int maybeUnusedPid = 0;

            for(int i = 0; i < size(); i++) {
                maybeUnusedReader = valueAt(i);
                maybeUnusedPid = keyAt(i);

                // skip dead entries
                if(maybeUnusedReader == null) {
                    continue;
                }

                // exit if we found an unused reader
                if(bundle.getStreamOfPid(maybeUnusedPid) == null) {
                    break;
                }

                maybeUnusedReader = null;
            }

            // found unused reader ?
            if(maybeUnusedReader != null) {
                remove(maybeUnusedPid);
                put(pid, new StreamReader(maybeUnusedReader.output(), stream));
                Log.i(TAG, "replaced unused stream " + maybeUnusedPid + " with new stream " + pid);
            }
            else {
                Log.i(TAG, "no more space for new stream " + pid);
            }
        }
    }
}
