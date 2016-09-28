package org.xvdr.extractor;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

public class StreamManager extends SparseArray<StreamReader> {

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
            StreamReader reader = null;
            for(int i = 0; i < size(); i++) {
                int p = keyAt(i);
                reader = get(p);

                // skip dead entries
                if(reader == null) {
                    continue;
                }

                // skip if the pid of the reader is in the bundle
                int maybeUnusedPid = reader.stream.physicalId;
                if(bundle.getStreamOfPid(maybeUnusedPid) != null) {
                    reader = null;
                    continue;
                }
            }

            // found unused reader ?
            if(reader != null) {
                remove(reader.stream.physicalId);
                put(pid, new StreamReader(reader.output(), stream));
                Log.i(TAG, "replaced unused stream " + reader.stream.physicalId + " with new stream " + pid);
                return;
            }

            Log.i(TAG, "no more space for new stream " + pid);
        }
    }
}
