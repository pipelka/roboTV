package org.xvdr.robotv.tv;

import android.media.tv.TvTrackInfo;

import org.xvdr.robotv.client.StreamBundle;

public class TrackInfoMapper {

    static public TvTrackInfo streamToTrackInfo(StreamBundle.Stream stream) {
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
                builder.setVideoFrameRate(stream.fpsRate / stream.fpsScale);
            }

            double stretchWidth = (double) stream.width * stream.pixelAspectRatio;

            builder.setVideoWidth((int)stretchWidth);
            builder.setVideoHeight(stream.height);

            return builder.build();
        }

        return null;
    }

    static public TvTrackInfo findTrackInfo(StreamBundle bundle, int contentType, int streamIndex) {
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
