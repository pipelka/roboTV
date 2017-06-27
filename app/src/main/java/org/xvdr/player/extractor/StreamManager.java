package org.xvdr.player.extractor;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.DefaultTrackOutput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.robotv.client.StreamBundle;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class StreamManager {

    private static final String TAG = "StreamManager";
    private StreamBundle bundle;
    private ExtractorOutput output;
    private int audioPid;
    private int videoPid;

    StreamManager(StreamBundle bundle) {
        this.bundle = bundle;
    }

    StreamBundle.Stream getStream(int pid) {
        return bundle.getStreamOfPid(pid);
    }

    TrackOutput getOutput(StreamBundle.Stream stream) {
        if(stream == null) {
            return null;
        }

        if(stream.physicalId == videoPid) {
            return output.track(0, C.TRACK_TYPE_VIDEO);
        }

        if(stream.physicalId == audioPid) {
            return output.track(1, C.TRACK_TYPE_AUDIO);
        }

        return null;
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

    private static int getScoreFromFormat(Format format, String audioLanguage) {
        if(format == null) {
            return -1;
        }

        int trackScore = 0;

        if(!TextUtils.isEmpty(audioLanguage) && !TextUtils.isEmpty(format.language)) {
            trackScore += (format.language.equals(audioLanguage) ? 20 : 0);
        }
        trackScore += (format.sampleMimeType.equals(MimeTypes.AUDIO_AC3) ? 1 : 0);
        trackScore += (format.sampleMimeType.equals(MimeTypes.AUDIO_E_AC3) ? 2 : 0);
        trackScore += format.channelCount;

        return trackScore;
    }

    private StreamBundle.Stream findVideoStream() {
        for(StreamBundle.Stream stream : bundle) {
            if (!isSteamSupported(stream)) {
                continue;
            }

            if (stream.isVideo()) {
                return stream;
            }
        }

        return null;
    }

    private StreamBundle.Stream findAudioStream(String audioLanguage) {
        StreamBundle.Stream bestStream = null;
        int bestScore = -1;

        for(StreamBundle.Stream stream : bundle) {
            if(!stream.isAudio()) {
                continue;
            }

            Format format = createFormat(stream, null);
            int score = getScoreFromFormat(format, audioLanguage);

            if(score > bestScore) {
                bestStream = stream;
                bestScore = score;
            }
        }

        return bestStream;
    }

    private static Format createFormat(StreamBundle.Stream stream, List<byte[]> initializationData) {
        String mimeType = stream.getMimeType();

        if(stream.isAudio()) {
            return Format.createAudioSampleFormat(
                    Integer.toString(stream.physicalId),
                    mimeType,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    stream.channels,
                    stream.sampleRate,
                    C.ENCODING_PCM_16BIT,
                    null, null,
                    0,
                    stream.language);
        }

        if(stream.isVideo()) {
            return Format.createVideoSampleFormat(
                    Integer.toString(stream.physicalId), // << trackId
                    mimeType,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    stream.width,
                    stream.height,
                    stream.getFrameRate(),
                    initializationData,
                    0,
                    (float)stream.pixelAspectRatio,
                    null);
        }

        return null;
    }

    Format getAudioFormat() {
        StreamBundle.Stream stream = getStream(audioPid);

        if(stream == null) {
            return null;
        }

        return createFormat(stream, null);
    }

    void createStreams(ExtractorOutput output, final String audioLanguage, boolean audioPassthrough) {
        this.output = output;
        TrackOutput track;
        boolean havePassthroughStreams = false;

        // check if we have passthrough audio tracks
        for(StreamBundle.Stream stream: bundle) {
            if(bundle.isPassthrough(stream.type)) {
                havePassthroughStreams = true;
                break;
            }
        }

        // remove non-passthrough streams
        if(havePassthroughStreams && audioPassthrough) {
            Iterator<StreamBundle.Stream> it = bundle.iterator();
            while (it.hasNext()) {
                StreamBundle.Stream stream = it.next();
                if(stream.isAudio() && !bundle.isPassthrough(stream.type)) {
                    it.remove();
                }
            }
        }

        // sort streams
        Collections.sort(bundle, new Comparator<StreamBundle.Stream>() {
            @Override
            public int compare(StreamBundle.Stream o1, StreamBundle.Stream o2) {
                return
                        getScoreFromFormat(createFormat(o2, null), audioLanguage) -
                        getScoreFromFormat(createFormat(o1, null), audioLanguage);
            }
        });

        // add video stream
        StreamBundle.Stream stream = findVideoStream();

        if(stream != null) {
            Log.i(TAG, "video track pid: " + stream.physicalId);
            videoPid = stream.physicalId;
            track = output.track(0, C.TRACK_TYPE_VIDEO);
            track.format(createFormat(stream, null));
        }

        // add audio stream
        stream = findAudioStream(audioLanguage);

        if(stream != null) {
            Log.i(TAG, "audio track pid: " + stream.physicalId);
            audioPid = stream.physicalId;
            track = output.track(1, C.TRACK_TYPE_AUDIO);
            track.format(createFormat(stream, null));
        }

        output.endTracks();
    }

    Format selectAudioTrack(int pid) {
        Log.d(TAG, "selectAudioTrack: " + pid);

        StreamBundle.Stream stream = bundle.getStreamOfPid(pid);

        if(stream == null || !stream.isAudio()) {
            Log.d(TAG, "not an audio stream !");
            return null;
        }

        DefaultTrackOutput track = (DefaultTrackOutput) output.track(1, C.TRACK_TYPE_AUDIO);
        Format format = createFormat(stream, null);
        track.disable();
        track.reset(true);
        track.format(format);

        audioPid = stream.physicalId;
        return format;
    }
}
