package org.robotv.tv;

import android.media.tv.TvTrackInfo;
import android.util.Log;

import org.robotv.player.StreamBundle;

class TrackInfoMapper {

    final private static String TAG = "TrackInfoMapper";

    private static TvTrackInfo streamToTrackInfo(StreamBundle.Stream stream) {
        if(stream.content == StreamBundle.CONTENT_AUDIO) {
            return new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(stream.physicalId))
                   .setAudioChannelCount(stream.channels)
                   .setAudioSampleRate(stream.sampleRate)
                   .setLanguage(stream.language)
                   .build();
        }
        else if(stream.content == StreamBundle.CONTENT_VIDEO) {
            TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, Integer.toString(stream.physicalId));

            if(stream.fpsScale != 0 && stream.fpsRate != 0) {
                builder.setVideoFrameRate((float)stream.fpsRate / stream.fpsScale);
            }

            int stretchWidth = (int)((double) stream.width * stream.pixelAspectRatio);
            float pixelAspect = (float)stream.pixelAspectRatio;

            builder.setVideoWidth(stream.width);
            builder.setVideoHeight(stream.height);
            builder.setVideoPixelAspectRatio(pixelAspect);

            builder.setVideoActiveFormatDescription((byte)8); // full frame image

            Log.i(TAG, "native size: " + stream.width + "x" + stream.height);
            Log.i(TAG, "pixel aspect ratio: " + stream.pixelAspectRatio);
            Log.i(TAG, "video size: " + stretchWidth + "x" + stream.height);

            return builder.build();
        }

        return null;
    }

    static TvTrackInfo findTrackInfo(StreamBundle bundle, int contentType, int streamIndex) {
        if(streamIndex < 0) {
            return null;
        }

        int count = 0;

        for(int i = 0; i < bundle.size(); i++) {
            StreamBundle.Stream stream = bundle.get(i);

            if(stream.content == contentType) {
                if(count == streamIndex) {
                    return streamToTrackInfo(stream);
                }

                count++;
            }
        }

        return null;
    }
}
