package org.xvdr.tv;

import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.Packet;

/**
 * Created by pipelka on 22.05.15.
 */
public class StreamBundle extends SparseArray<StreamBundle.Stream> {
	static final String TAG = "StreamBundle";
    Uri mChannelUri;

	public class Stream {

		public final static String CONTENT_AUDIO = "AUDIO";
		public final static String CONTENT_VIDEO = "VIDEO";
		public final static String CONTENT_SUBTITLE = "SUBTITLE";
		public final static String CONTENT_TELETEXT = "TELETEXT";
		public final static String CONTENT_UNKNOWN = "UNKNOWN";

		public final static String TYPE_AUDIO_AC3 = "AC3";
		public final static String TYPE_AUDIO_EAC3 = "EAC3";
		public final static String TYPE_AUDIO_AAC = "AAC";
		public final static String TYPE_AUDIO_MPEG2 = "MPEG2AUDIO";

		public final static String TYPE_VIDEO_MPEG2 = "MPEG2VIDEO";
		public final static String TYPE_VIDEO_H264 = "H264";

		public final static String TYPE_SUBTITLE = "DVBSUB";
		public final static String TYPE_TELETEXT = "TELETEXT";

		int index;
		public int physicalId;
		public String type;
        public String content;
        public long identifier = -1;
		public String language;
        public int channels;
        public int sampleRate;
        public long blockAlign;
        public long bitRate;
        public long bitsPerSample;
        public long fpsScale;
        public long fpsRate;
		public int width;
		public int height;
		public double aspect;
		boolean synced = false;

        TvTrackInfo trackInfo;

		public String getMimeType() {
			return getMimeTypeFromType(type);
		}

        final public TvTrackInfo getTrackInfo() {
            return trackInfo;
        }
	}

    public StreamBundle(Uri channelUri) {
        mChannelUri = channelUri;
    }

    public Uri getChannelUri() {
        return mChannelUri;
    }

    public Stream getVideoStream() {
        for(int i = 0; i < size(); i++) {
            if(valueAt(i).content.equals(Stream.CONTENT_VIDEO)) {
                return valueAt(i);
            }
        }

        return  null;
    }

	private static String getContentFromType(String type) {
		if(type.equals(Stream.TYPE_AUDIO_AC3)) {
			return Stream.CONTENT_AUDIO;
		}
		else if(type.equals(Stream.TYPE_AUDIO_MPEG2)) {
			return Stream.CONTENT_AUDIO;
		}
		else if(type.equals(Stream.TYPE_AUDIO_AAC)) {
			return Stream.CONTENT_AUDIO;
		}
		else if(type.equals(Stream.TYPE_AUDIO_EAC3)) {
			return Stream.CONTENT_AUDIO;
		}
		else if(type.equals(Stream.TYPE_VIDEO_MPEG2)) {
			return Stream.CONTENT_VIDEO;
		}
		else if(type.equals(Stream.TYPE_VIDEO_H264)) {
			return Stream.CONTENT_VIDEO;
		}
		else if(type.equals(Stream.TYPE_SUBTITLE)) {
			return Stream.CONTENT_SUBTITLE;
		}
		else if(type.equals(Stream.TYPE_TELETEXT)) {
			return Stream.CONTENT_TELETEXT;
		}
		else {
			return Stream.CONTENT_UNKNOWN;
		}
	}

	private static String getMimeTypeFromType(String type) {
		if(type.equals(Stream.TYPE_AUDIO_AC3)) {
			return MimeTypes.AUDIO_AC3;
		}
		else if(type.equals(Stream.TYPE_AUDIO_MPEG2)) {
			return "audio/mpeg";
		}
		else if(type.equals(Stream.TYPE_AUDIO_AAC)) {
			return MimeTypes.AUDIO_AAC;
		}
		else if(type.equals(Stream.TYPE_AUDIO_EAC3)) {
			return MimeTypes.AUDIO_AC3;
		}
		else if(type.equals(Stream.TYPE_VIDEO_MPEG2)) {
			return "video/mpeg";
		}
		else if(type.equals(Stream.TYPE_VIDEO_H264)) {
			return MimeTypes.VIDEO_H264;
		}
		else if(type.equals(Stream.TYPE_SUBTITLE)) {
			return "text/vnd.dvb.subtitle";
		}
		else if(type.equals(Stream.TYPE_TELETEXT)) {
			return "teletext";
		}
		else {
			return "unknown";
		}
	}

	public synchronized void updateFromPacket(Packet p) {
		if(p.getMsgID() != ServerConnection.XVDR_STREAM_CHANGE) {
			return;
		}

		int index = 0;

		while(!p.eop()) {
			Stream stream = new Stream();

			stream.index = index;
			stream.physicalId = (int)p.getU32();
			stream.type = p.getString();
			stream.content = getContentFromType(stream.type);
			stream.synced = false;

			if(stream.content == Stream.CONTENT_AUDIO) {
				stream.language = p.getString();
				stream.channels = (int) p.getU32();
				stream.sampleRate = (int) p.getU32();
				stream.blockAlign = p.getU32();
				stream.bitRate = p.getU32();
				stream.bitsPerSample = p.getU32();

                stream.trackInfo = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(stream.physicalId))
                        .setAudioChannelCount(stream.channels)
                        .setAudioSampleRate(stream.sampleRate)
                        .setLanguage(stream.language)
                        .build();
			}
			else if(stream.content == Stream.CONTENT_VIDEO) {
				stream.fpsScale = p.getU32();
				stream.fpsRate = p.getU32();
				stream.height = (int) p.getU32();
				stream.width = (int) p.getU32();
				stream.aspect = (float)(p.getS64() / 10000.0);

                stream.trackInfo = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, Integer.toString(stream.physicalId))
                        .setVideoFrameRate(stream.fpsRate / stream.fpsScale)
                        .setVideoWidth(stream.width)
                        .setVideoHeight(stream.height)
                        .build();
			}
			else if(stream.content == Stream.CONTENT_SUBTITLE) {
				stream.language = p.getString();
				long composition_id = p.getU32();
				long ancillary_id = p.getU32();
				stream.identifier = (composition_id & 0xffff) | ((ancillary_id & 0xffff) << 16);
				index++;
				continue;
			}
			else if(stream.content == Stream.CONTENT_TELETEXT) {
				stream.language = p.getString();
				long composition_id = p.getU32();
				long ancillary_id = p.getU32();
				stream.identifier = (composition_id & 0xffff) | ((ancillary_id & 0xffff) << 16);
				continue;
			}
			else {
				continue;
			}

			Log.i(TAG, "new " + stream.content + " " + stream.type + " stream " + index + " (" + stream.physicalId + ")");
			put(stream.physicalId, stream);

			index++;
		}

	}


}
