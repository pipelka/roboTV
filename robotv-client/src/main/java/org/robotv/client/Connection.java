package org.robotv.client;

import android.util.Log;

import org.robotv.msgexchange.Packet;
import org.robotv.msgexchange.Session;

public class Connection extends Session {

    private static final String TAG = "XVDR";

    public final static int STATUS_NORESPONSE = -1;
    public final static int STATUS_SUCCESS = 0;
    public final static int STATUS_RECEIVERS_BUSY = 997;
    public final static int STATUS_BLOCKED_BY_RECORDING = 1;
    public final static int STATUS_CONNECTION_FAILED = 2;

    private final String mSessionName;
    private String mLanguage = "deu";
    private boolean mEnableStatus = false;

    /** TCP/IP Port **/
    public static final int TCPIP_PORT = 34892;

    /** Frame types */
    public static final int IFRAME = 1;

    /** Packet types */
    public static final int CHANNEL_REQUEST_RESPONSE = 1;
    public static final int CHANNEL_STREAM = 2;
    public static final int CHANNEL_STATUS = 5;


    /* OPCODE 1 - 19: network functions for general purpose */
    public static final int LOGIN = 1;
    public static final int GET_CONFIG = 8;

    /* OPCODE 20 - 39: network functions for live streaming */
    public static final int CHANNELSTREAM_OPEN = 20;
    public static final int CHANNELSTREAM_CLOSE = 21;
    public static final int CHANNELSTREAM_REQUEST = 22;
    public static final int CHANNELSTREAM_PAUSE = 23;
    public static final int CHANNELSTREAM_SIGNAL = 24;
    public static final int CHANNELSTREAM_SEEK = 25;

    public static final int RECSTREAM_OPEN = 40;
    public static final int RECSTREAM_CLOSE = 41;

    /* OPCODE 60 - 79: network functions for channel access */
    public static final int CHANNELS_GETCHANNELS = 63;

    /* OPCODE 80 - 99: network functions for timer access */
    public static final int TIMER_GETLIST = 82;
    public static final int TIMER_ADD = 83;
    public static final int TIMER_DELETE = 84;
    public static final int TIMER_UPDATE = 85;
    public static final int SEARCHTIMER_GETLIST = 86;
    public static final int SEARCHTIMER_ADD = 87;
    public static final int SEARCHTIMER_DELETE = 88;

    /* OPCODE 100 - 119: network functions for recording access */
    public static final int RECORDINGS_DISKSIZE = 100;
    public static final int RECORDINGS_GETFOLDERS = 101;
    public static final int RECORDINGS_GETLIST = 102;
    public static final int RECORDINGS_RENAME = 103;
    public static final int RECORDINGS_DELETE = 104;
    public static final int RECORDINGS_SETPLAYCOUNT = 105;
    public static final int RECORDINGS_SETPOSITION = 106;
    public static final int RECORDINGS_GETPOSITION = 107;
    public static final int RECORDINGS_GETMARKS = 108;
    public static final int RECORDINGS_SETURLS = 109;
    public static final int RECORDINGS_SEARCH = 112;
    public static final int RECORDINGS_GETMOVIE = 113;

    public static final int ARTWORK_SET = 110;
    public static final int ARTWORK_GET = 111;

    /* OPCODE 120 - 139: network functions for epg access and manipulating */
    public static final int EPG_GETFORCHANNEL = 120;
    public static final int EPG_SEARCH = 121;

    /** Stream packet types (server -> client) */
    public static final int STREAM_CHANGE = 1;
    public static final int STREAM_STATUS = 2;
    public static final int STREAM_QUEUESTATUS = 3;
    public static final int STREAM_MUXPKT = 4;
    public static final int STREAM_SIGNALINFO = 5;
    public static final int STREAM_DETACH = 7;
    public static final int STREAM_POSITIONS = 8;

    /** Status packet types (server -> client) */
    public static final int STATUS_TIMERCHANGE = 1;
    public static final int STATUS_RECORDING = 2;
    public static final int STATUS_MESSAGE = 3;
    public static final int STATUS_CHANNELCHANGE = 4;
    public static final int STATUS_RECORDINGSCHANGE = 5;

    public static final int PROTOCOLVERSION = 8;

    public Packet CreatePacket(int msgid, int type) {
        return new Packet(msgid, type);
    }

    public Packet CreatePacket(int msgid) {
        return new Packet(msgid, CHANNEL_REQUEST_RESPONSE);
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
        setTimeout(10000);

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
        Packet req = CreatePacket(LOGIN);

        req.setProtocolVersion(PROTOCOLVERSION);
        req.putU8((short) 0);
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
        Packet req = CreatePacket(Connection.CHANNELSTREAM_OPEN, Connection.CHANNEL_REQUEST_RESPONSE);

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
        Packet req = CreatePacket(Connection.RECSTREAM_OPEN, Connection.CHANNEL_REQUEST_RESPONSE);
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
        Packet req = CreatePacket(Connection.CHANNELSTREAM_CLOSE, Connection.CHANNEL_REQUEST_RESPONSE);
        return (transmitMessage(req) != null);
    }

    public String getConfig(String key) {
        Packet request = CreatePacket(GET_CONFIG);
        request.putString(key);

        Packet response = transmitMessage(request);

        if(response == null) {
            return "";
        }

        return response.getString();
    }

    public boolean seek(long position) {
        Packet req = CreatePacket(Connection.CHANNELSTREAM_SEEK, Connection.CHANNEL_REQUEST_RESPONSE);
        req.putS64(position);

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return false;
        }

        resp.getS64();
        return true;
    }

    public int deleteRecording(int id) {
        Packet req = CreatePacket(Connection.RECORDINGS_DELETE);
        req.putString(Integer.toString(id, 16));

        Packet resp = transmitMessage(req);

        if(resp == null) {
            return -1;
        }

        return (int)resp.getU32();
    }

    public int renameRecording(int id, String newName) {
        Packet req = CreatePacket(Connection.RECORDINGS_RENAME);
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
