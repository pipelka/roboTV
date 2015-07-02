package org.xvdr.robotv.tv;

import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.google.android.exoplayer.util.MimeTypes;

import org.xvdr.msgexchange.Packet;

/**
 * Created by pipelka on 22.05.15.
 */
public class StreamBundle extends SparseArray<StreamBundle.Stream> {
	static final String TAG = "StreamBundle";
    Uri mChannelUri;

    static private final SparseArray<String> typeMapping = new SparseArray<String>() {
        {
            append(TYPE_AUDIO_MPEG2, "MPEG2AUDIO");
            append(TYPE_AUDIO_AC3,   "AC3");
            append(TYPE_AUDIO_EAC3,  "EAC3");
            append(TYPE_AUDIO_AAC,   "AAC");
            append(TYPE_AUDIO_LATM,  "LATM");
            append(TYPE_VIDEO_MPEG2, "MPEG2VIDEO");
            append(TYPE_VIDEO_H264,  "H264");
            append(TYPE_SUBTITLE,    "DVBSUB");
            append(TYPE_TELETEXT,    "TELETEXT");
        }
    };

    static private final SparseIntArray contentMapping = new SparseIntArray() {
        {
            append(TYPE_AUDIO_MPEG2, CONTENT_AUDIO);
            append(TYPE_AUDIO_AC3,   CONTENT_AUDIO);
            append(TYPE_AUDIO_EAC3,  CONTENT_AUDIO);
            append(TYPE_AUDIO_AAC,   CONTENT_AUDIO);
            append(TYPE_AUDIO_LATM,  CONTENT_AUDIO);
            append(TYPE_VIDEO_MPEG2, CONTENT_VIDEO);
            append(TYPE_VIDEO_H264,  CONTENT_VIDEO);
            append(TYPE_SUBTITLE,    CONTENT_SUBTITLE);
            append(TYPE_TELETEXT,    CONTENT_TELETEXT);
        }
    };

    public final static int CONTENT_UNKNOWN = 0;
    public final static int CONTENT_AUDIO = 1;
    public final static int CONTENT_VIDEO = 2;
    public final static int CONTENT_SUBTITLE = 3;
    public final static int CONTENT_TELETEXT = 4;

    public final static int TYPE_UNKNOWN = 0;

    public final static int TYPE_AUDIO_MPEG2 = 1;
    public final static int TYPE_AUDIO_AC3 = 2;
    public final static int TYPE_AUDIO_EAC3 = 3;
    public final static int TYPE_AUDIO_AAC = 4;
    public final static int TYPE_AUDIO_LATM = 5;

    public final static int TYPE_VIDEO_MPEG2 = 6;
    public final static int TYPE_VIDEO_H264 = 7;

    public final static int TYPE_SUBTITLE = 8;
    public final static int TYPE_TELETEXT = 9;

    private final static int supportedTypes[] = {
            TYPE_VIDEO_H264,
            TYPE_AUDIO_AC3,
            TYPE_AUDIO_AAC
    };

    public class Stream {

		public int physicalId;
		public int type;
        public int content;
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

		public String getMimeType() {
			return getMimeTypeFromType(type);
		}

        final public TvTrackInfo getTrackInfo() {
            if(content == CONTENT_AUDIO) {
                return new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(physicalId))
                        .setAudioChannelCount(channels)
                        .setAudioSampleRate(sampleRate)
                        .setLanguage(language)
                        .build();
            }
            else if(content == CONTENT_VIDEO) {
                return new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, Integer.toString(physicalId))
                        .setVideoFrameRate(fpsRate / fpsScale)
                        .setVideoWidth(width)
                        .setVideoHeight(height)
                        .build();
            }

            return null;
        }
	}

    public StreamBundle(Uri channelUri) {
        mChannelUri = channelUri;
    }

    public Uri getChannelUri() {
        return mChannelUri;
    }

    public boolean isTypeSupported(int type) {
        for(int i = 0; i < supportedTypes.length; i++) {
            if(supportedTypes[i] == type) {
                return true;
            }
        }
        return false;
    }

    private int getTypeFromName(String typeName) {
        for(int i = 0; i < typeMapping.size(); i++) {
            String name = typeMapping.valueAt(i);
            if(name.equals(typeName)) {
                return typeMapping.keyAt(i);
            }
        }

        return -1;
    }

    public Stream getVideoStream() {
        for(int i = 0; i < size(); i++) {
            if(valueAt(i).content == CONTENT_VIDEO) {
                return valueAt(i);
            }
        }

        return  null;
    }

    public Stream getDefaultAudioStream() {
        for(int i = 0; i < size(); i++) {
            if(valueAt(i).content == CONTENT_AUDIO) {
                return valueAt(i);
            }
        }

        return  null;
    }

    private static String getMimeTypeFromType(int type) {
		if(type == TYPE_AUDIO_AC3) {
			return MimeTypes.AUDIO_AC3;
		}
		else if(type == TYPE_AUDIO_MPEG2) {
			return MimeTypes.AUDIO_MPEG;
		}
		else if(type == TYPE_AUDIO_AAC) {
			return MimeTypes.AUDIO_AAC;
		}
		else if(type == TYPE_AUDIO_EAC3) {
			return MimeTypes.AUDIO_EC3;
		}
		else if(type == TYPE_VIDEO_MPEG2) {
			return "video/mpeg";
		}
		else if(type == TYPE_VIDEO_H264) {
			return MimeTypes.VIDEO_H264;
		}
		else if(type == TYPE_SUBTITLE) {
			return "text/vnd.dvb.subtitle";
		}
		else if(type == TYPE_TELETEXT) {
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

        clear();

		while(!p.eop()) {
			Stream stream = new Stream();

			stream.physicalId = (int)p.getU32();
            stream.type = getTypeFromName(p.getString());
			stream.content = contentMapping.get(stream.type);

			if(stream.content == CONTENT_AUDIO) {
				stream.language = p.getString();
				stream.channels = (int) p.getU32();
				stream.sampleRate = (int) p.getU32();
				stream.blockAlign = p.getU32();
				stream.bitRate = p.getU32();
				stream.bitsPerSample = p.getU32();
			}
			else if(stream.content == CONTENT_VIDEO) {
				stream.fpsScale = p.getU32();
				stream.fpsRate = p.getU32();
				stream.height = (int) p.getU32();
				stream.width = (int) p.getU32();
				stream.aspect = (float)(p.getS64() / 10000.0);
			}
			else if(stream.content == CONTENT_SUBTITLE) {
				stream.language = p.getString();
				long composition_id = p.getU32();
				long ancillary_id = p.getU32();
				stream.identifier = (composition_id & 0xffff) | ((ancillary_id & 0xffff) << 16);
				continue;
			}
			else if(stream.content == CONTENT_TELETEXT) {
				stream.language = p.getString();
				long composition_id = p.getU32();
				long ancillary_id = p.getU32();
				stream.identifier = (composition_id & 0xffff) | ((ancillary_id & 0xffff) << 16);
				continue;
			}
			else {
				continue;
			}

            // skip unsupported stream types
            if(!isTypeSupported(stream.type)) {
                continue;
            }

            Log.i(TAG, "new " + contentMapping.get(stream.content) + " " + typeMapping.get(stream.type) + " stream (" + stream.physicalId + ")");
			put(stream.physicalId, stream);
		}

	}


}
