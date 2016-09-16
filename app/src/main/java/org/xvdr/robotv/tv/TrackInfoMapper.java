package org.xvdr.robotv.tv;

import android.media.tv.TvTrackInfo;

import org.xvdr.robotv.client.StreamBundle;

public class TrackInfoMapper {

    static public TvTrackInfo streamToTrackInfo(StreamBundle.Stream stream, int displayWidth, int displayHeight) {
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
            double factor = 1.0;

            // scale down picture (if larger than display)
            if(stream.width != 0 && stretchWidth > displayWidth) {
                factor = displayWidth / stretchWidth;
            }

            builder.setVideoWidth((int)(stretchWidth * factor));
            builder.setVideoHeight((int)(stream.height * factor));

            return builder.build();
        }

        return null;
    }

    static public TvTrackInfo streamToTrackInfo(StreamBundle.Stream stream) {
        return streamToTrackInfo(stream, 0, 0);
    }

    static public TvTrackInfo findTrackInfo(StreamBundle bundle, int contentType, int streamIndex, int width, int height) {
        if(streamIndex < 0) {
            return null;
        }

        int count = 0;

        for(int i = 0; i < bundle.size(); i++) {
            StreamBundle.Stream stream = bundle.get(i);

            if(stream.content == contentType) {
                if(count == streamIndex) {
                    return streamToTrackInfo(stream, width, height);
                }

                count++;
            }
        }

        return null;
    }

    static public TvTrackInfo findTrackInfo(StreamBundle bundle, int contentType, int streamIndex) {
        return findTrackInfo(bundle, contentType, streamIndex, 0, 0);
    }
}
