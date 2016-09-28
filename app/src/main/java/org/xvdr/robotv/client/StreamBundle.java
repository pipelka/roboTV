package org.xvdr.robotv.client;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.google.android.exoplayer2.util.MimeTypes;

import org.xvdr.extractor.BufferPacket;
import org.xvdr.jniwrap.Packet;

import java.util.ArrayList;

public class StreamBundle extends ArrayList<StreamBundle.Stream> {
    static final String TAG = "StreamBundle";

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
            append(TYPE_VIDEO_H265,  "H265");
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
            append(TYPE_VIDEO_H265,  CONTENT_VIDEO);
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
    public final static int TYPE_VIDEO_H265 = 10;

    public final static int TYPE_SUBTITLE = 8;
    public final static int TYPE_TELETEXT = 9;

    private final static int supportedTypes[] = {
        TYPE_VIDEO_MPEG2,
        TYPE_VIDEO_H264,
        TYPE_VIDEO_H265,
        TYPE_AUDIO_AC3,
        TYPE_AUDIO_AAC,
        TYPE_AUDIO_MPEG2
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
        public int bitRate;
        public long bitsPerSample;
        public long fpsScale;
        public long fpsRate;
        public int width;
        public int height;
        public double pixelAspectRatio;
        public int spsLength;
        public byte[] sps = new byte[128];
        public int ppsLength;
        public byte[] pps = new byte[128];
        public int vpsLength;
        public byte[] vps = new byte[128];

        public boolean isEqualTo(Stream s) {
            return
                this.physicalId == s.physicalId &&
                this.channels == s.channels &&
                this.sampleRate == s.sampleRate &&
                this.width == s.width &&
                this.height == s.height &&
                this.pixelAspectRatio == s.pixelAspectRatio &&
                this.type == s.type;
        }

        public String getMimeType() {
            return getMimeTypeFromType(type);
        }

        public float getFrameRate() {
            if(fpsScale != 0 && fpsRate != 0) {
                return fpsRate / fpsScale;
            }

            return 0;
        }
    }

    public StreamBundle() {
    }

    public boolean isEqualTo(StreamBundle b) {
        if(size() != b.size()) {
            return false;
        }

        for(int i = 0; i < size(); i++) {
            Stream stream = b.get(i);

            if(stream == null) {
                return false;
            }

            if(!this.get(i).isEqualTo(stream)) {
                return false;
            }
        }

        return true;
    }

    public boolean isTypeSupported(int type) {
        for(int supportedType : supportedTypes) {
            if(supportedType == type) {
                return true;
            }
        }

        return false;
    }

    private int getTypeFromName(String typeName) {
        if(typeName == null) {
            return -1;
        }

        for(int i = 0; i < typeMapping.size(); i++) {
            String name = typeMapping.valueAt(i);

            if(name.equals(typeName)) {
                return typeMapping.keyAt(i);
            }
        }

        return -1;
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
            return MimeTypes.AUDIO_E_AC3;
        }
        else if(type == TYPE_VIDEO_MPEG2) {
            return MimeTypes.VIDEO_MPEG2;
        }
        else if(type == TYPE_VIDEO_H264) {
            return MimeTypes.VIDEO_H264;
        }
        else if(type == TYPE_VIDEO_H265) {
            return MimeTypes.VIDEO_H265;
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

    public synchronized void updateFromPacket(BufferPacket p) {
        clear();

        int streamCount = p.getU8();
        Log.d(TAG, "new streamchange: " + streamCount + " entries");

        for(int i = 0; i < streamCount; i++) {
            Log.d(TAG, "stream: " + i);
            Stream stream = new Stream();

            stream.physicalId = (int)p.getU32();
            String typeName = p.getString();
            Log.d(TAG, "type: " + typeName);
            stream.type = getTypeFromName(typeName);
            stream.content = contentMapping.get(stream.type);

            if(stream.content == CONTENT_AUDIO) {
                stream.language = p.getString();
                stream.channels = (int) p.getU32();
                stream.sampleRate = (int) p.getU32();
                stream.blockAlign = p.getU32();
                stream.bitRate = (int)p.getU32();
                stream.bitsPerSample = p.getU32();
            }
            else if(stream.content == CONTENT_VIDEO) {
                stream.fpsScale = p.getU32();
                stream.fpsRate = p.getU32();
                stream.height = (int) p.getU32();
                stream.width = (int) p.getU32();

                double aspect = (float)(p.getS64() / 10000.0);

                if(aspect < 1.0) {
                    aspect = 1.0;
                }

                // roboTV sends the picture aspect ratio
                // we have to convert it to the pixel aspect ratio
                double value = (aspect * stream.height) / (double)stream.width;
                stream.pixelAspectRatio = (double)Math.round(value * 1000) / 1000;

                stream.spsLength = p.getU8();

                if(stream.spsLength > 0) {
                    p.readBuffer(stream.sps, 0, stream.spsLength);
                }

                stream.ppsLength = p.getU8();

                if(stream.ppsLength > 0) {
                    p.readBuffer(stream.pps, 0, stream.ppsLength);
                }

                stream.vpsLength = p.getU8();

                if(stream.vpsLength > 0) {
                    p.readBuffer(stream.vps, 0, stream.vpsLength);
                }
            }
            else if(stream.content == CONTENT_SUBTITLE) {
                stream.language = p.getString();
                long composition_id = p.getU32();
                long ancillary_id = p.getU32();
                stream.identifier = (composition_id & 0xffff) | ((ancillary_id & 0xffff) << 16);
                continue;
            }
            else if(stream.content == CONTENT_TELETEXT) {
                continue;
            }
            else {
                continue;
            }

            // skip unsupported stream types
            if(!isTypeSupported(stream.type)) {
                continue;
            }

            Log.i(TAG, "new " + stream.content + " " + typeMapping.get(stream.type) + " stream (" + stream.physicalId + ")");
            add(stream);
        }

    }

    public int getStreamCount(int contentType) {
        int count = 0;

        for(int i = 0; i < size(); i++) {
            Stream stream = get(i);

            if(stream.content == contentType) {
                count++;
            }
        }

        return count;
    }

    public Stream getStream(int contentType, int streamIndex) {
        if(streamIndex < 0) {
            return null;
        }

        int count = 0;

        for(int i = 0; i < size(); i++) {
            Stream stream = get(i);

            if(stream == null) {
                continue;
            }

            if(stream.content == contentType) {
                if(count == streamIndex) {
                    return stream;
                }

                count++;
            }
        }

        return null;
    }

    public Stream getStreamOfPid(int pid) {
        for(int i = 0; i < size(); i++) {
            Stream stream = get(i);

            if(stream.physicalId == pid) {
                return stream;
            }
        }

        return null;
    }

    public int findIndexByPhysicalId(int contentType, int physicalId) {
        int index = 0;

        for(int i = 0; i < size(); i++) {
            Stream stream = get(i);

            if(stream.content == contentType) {
                if(stream.physicalId == physicalId) {
                    return index;
                }

                index++;
            }
        }

        return -1;
    }
}
