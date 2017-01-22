package org.xvdr.robotv.client;

import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.Session;

public class Connection extends Session {

    private static final String TAG = "XVDR";

    public final static int STATUS_NORESPONSE = -1;
    public final static int STATUS_SUCCESS = 0;
    public final static int STATUS_RECEIVERS_BUSY = 997;
    public final static int STATUS_BLOCKED_BY_RECORDING = 1;
    public final static int STATUS_CONNECTION_FAILED = 2;

    private String mSessionName = "XVDR Client";
    private short mCompressionLevel = 0;
    private int mAudioType = StreamBundle.TYPE_AUDIO_AC3;
    private String mLanguage = "deu";
    private boolean mEnableStatus = false;

    /** TCP/IP Port **/
    public static final int TCPIP_PORT = 34892;

    /** Frame types */
    public static final int IFRAME = 1;

    /** Packet types */
    public static final int XVDR_CHANNEL_REQUEST_RESPONSE = 1;
    public static final int XVDR_CHANNEL_STREAM = 2;
    public static final int XVDR_CHANNEL_STATUS = 5;


    /* OPCODE 1 - 19: XVDR network functions for general purpose */
    public static final int XVDR_LOGIN = 1;
    public static final int XVDR_GET_CONFIG = 8;

    /* OPCODE 20 - 39: XVDR network functions for live streaming */
    public static final int XVDR_CHANNELSTREAM_OPEN = 20;
    public static final int XVDR_CHANNELSTREAM_CLOSE = 21;
    public static final int XVDR_CHANNELSTREAM_REQUEST = 22;
    public static final int XVDR_CHANNELSTREAM_PAUSE = 23;
    public static final int XVDR_CHANNELSTREAM_SIGNAL = 24;
    public static final int XVDR_CHANNELSTREAM_SEEK = 25;

    public static final int XVDR_RECSTREAM_OPEN = 40;
    public static final int XVDR_RECSTREAM_CLOSE = 41;

    /* OPCODE 60 - 79: XVDR network functions for channel access */
    public static final int XVDR_CHANNELS_GETCHANNELS = 63;

    /* OPCODE 80 - 99: RoboTV network functions for timer access */
    public static final int ROBOTV_TIMER_GETLIST = 82;
    public static final int ROBOTV_TIMER_ADD = 83;
    public static final int ROBOTV_TIMER_DELETE = 84;
    public static final int ROBOTV_TIMER_UPDATE = 85;
    public static final int ROBOTV_SEARCHTIMER_GETLIST = 86;

    /* OPCODE 100 - 119: XVDR network functions for recording access */
    public static final int XVDR_RECORDINGS_DISKSIZE = 100;
    public static final int XVDR_RECORDINGS_GETFOLDERS = 101;
    public static final int XVDR_RECORDINGS_GETLIST = 102;
    public static final int XVDR_RECORDINGS_RENAME = 103;
    public static final int XVDR_RECORDINGS_DELETE = 104;
    public static final int XVDR_RECORDINGS_SETPLAYCOUNT = 105;
    public static final int XVDR_RECORDINGS_SETPOSITION = 106;
    public static final int XVDR_RECORDINGS_GETPOSITION = 107;
    public static final int XVDR_RECORDINGS_GETMARKS = 108;
    public static final int XVDR_RECORDINGS_SETURLS = 109;
    public static final int XVDR_RECORDINGS_SEARCH = 112;

    public static final int XVDR_ARTWORK_SET = 110;
    public static final int XVDR_ARTWORK_GET = 111;

    /* OPCODE 120 - 139: XVDR network functions for epg access and manipulating */
    public static final int XVDR_EPG_GETFORCHANNEL = 120;
    public static final int XVDR_EPG_SEARCH = 121;

    /** Stream packet types (server -> client) */
    public static final int XVDR_STREAM_CHANGE = 1;
    public static final int XVDR_STREAM_STATUS = 2;
    public static final int XVDR_STREAM_QUEUESTATUS = 3;
    public static final int XVDR_STREAM_MUXPKT = 4;
    public static final int XVDR_STREAM_SIGNALINFO = 5;
    public static final int XVDR_STREAM_DETACH = 7;
    public static final int XVDR_STREAM_POSITIONS = 8;

    /** Status packet types (server -> client) */
    public static final int XVDR_STATUS_TIMERCHANGE = 1;
    public static final int XVDR_STATUS_RECORDING = 2;
    public static final int XVDR_STATUS_MESSAGE = 3;
    public static final int XVDR_STATUS_CHANNELCHANGE = 4;
    public static final int XVDR_STATUS_RECORDINGSCHANGE = 5;

    public static final int XVDRPROTOCOLVERSION = 8;

    public Packet CreatePacket(int msgid, int type) {
        return new Packet(msgid, type);
    }

    public Packet CreatePacket(int msgid) {
        return new Packet(msgid, XVDR_CHANNEL_REQUEST_RESPONSE);
    }

    public Connection(String sessionName) {
        mSessionName = sessionName;
    }

    public Connection(String sessionName, String language) {
        this(sessionName, language, false);
    }

    public Connection(String sessionname, String language, boolean enableStatus) {
        mSessionName = sessionname;
        mLanguage = language;
        mEnableStatus = enableStatus;

        // set timeout to 5000ms
        setTimeout(5000);

    }

    public boolean open(String hostname) {
        if(!super.open(hostname, TCPIP_PORT)) {
            Log.e(TAG, "failed to open server connection");
            return false;
        }

        if(!login()) {
            Log.e(TAG, "failed to login");
            return false;
        }

        return true;
    }

    public boolean login() {
        Packet req = CreatePacket(XVDR_LOGIN);

        req.setProtocolVersion(XVDRPROTOCOLVERSION);
        req.putU8(mCompressionLevel);
        req.putString(mSessionName);
        req.putU8((short)(mEnableStatus ? 1 : 0));
        req.putU8((short)getPriority());

        // read welcome
        Packet resp = transmitMessage(req);

        if(resp == null) {
            Log.e(TAG, "failed to read greeting from server");
            return false;
        }

        int protocolVersion = resp.getProtocolVersion();
        long vdrTime = resp.getU32();
        long vdrTimeOffset = resp.getS32();
        String server = resp.getString();
        String version = resp.getString();

        Log.i(TAG, "Logged in at '" + vdrTime + "+" + vdrTimeOffset + "' to '" + server + "' Version: '" + version + "' with protocol version '" + protocolVersion + "'");
        Log.i(TAG, "Preferred Audio Language: " + mLanguage);

        return true;
    }

    /**
     * Start streaming of a LiveTV channel
     * @param channelUid the unique id of the channel
     * @param waitForKeyFrame start streaming after the first IFRAME has been received
     * @param priority priority of the received device on the server
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, String language, boolean waitForKeyFrame, int priority) {
        Packet req = CreatePacket(Connection.XVDR_CHANNELSTREAM_OPEN, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);

        req.putU32(channelUid);
        req.putS32(priority); // priority 50
        req.putU8((short)(waitForKeyFrame ? 1 : 0));  // start with IFrame
        req.putString(language);

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return STATUS_NORESPONSE;
        }

        return (int)resp.getU32();
    }

    public int openRecording(String recordingId, long position) {
        Packet req = CreatePacket(Connection.XVDR_RECSTREAM_OPEN, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putString(recordingId);

        Packet resp = transmitMessage(req);

        if(resp == null) {
            Log.e(TAG, "no response opening recording: " + recordingId);
            return Connection.STATUS_NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status != Connection.STATUS_SUCCESS || position == 0) {
            return status;
        }

        long seekTime = System.currentTimeMillis() + position;

        return seek(seekTime) ? STATUS_SUCCESS : STATUS_NORESPONSE;
    }

    public boolean closeStream() {
        Packet req = CreatePacket(Connection.XVDR_CHANNELSTREAM_CLOSE, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        return (transmitMessage(req) != null);
    }

    public String getConfig(String key) {
        Packet request = CreatePacket(XVDR_GET_CONFIG);
        request.putString(key);

        Packet response = transmitMessage(request);

        if(response == null) {
            return "";
        }

        return response.getString();
    }

    public boolean seek(long position) {
        Packet req = CreatePacket(Connection.XVDR_CHANNELSTREAM_SEEK, Connection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putS64(position);

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return false;
        }

        resp.getS64();
        return true;
    }

    public int deleteRecording(int id) {
        Packet req = CreatePacket(Connection.XVDR_RECORDINGS_DELETE);
        req.putString(Integer.toString(id, 16));

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return -1;
        }

        return (int)resp.getU32();
    }

    public int renameRecording(int id, String newName) {
        Packet req = CreatePacket(Connection.XVDR_RECORDINGS_RENAME);
        req.putString(Integer.toString(id, 16));
        req.putString(newName);

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return -1;
        }

        return (int)resp.getU32();
    }

    @Override
    public void delete() {
        Log.d(TAG, "Connection - delete()");
        close();
    }
}
