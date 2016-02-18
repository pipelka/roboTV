package org.xvdr.extractor;

import android.content.Context;
import android.os.Build;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tv.ServerConnection;

public class LiveTvPlayer extends Player {

    public LiveTvPlayer(Context context, String server, String language, Listener listener, boolean audioPassthrough, int wantedChannelConfiguration) {
        super(context, server, language, listener, audioPassthrough, wantedChannelConfiguration);
    }

    /**
     * Start streaming of a LiveTV channel with a default priority, without waiting for the first keyframe
     * @param channelUid the unique id of the channel
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, String language) {
        return openStream(channelUid, language, false);
    }

    /**
     * Start streaming of a LiveTV channel with a default priority
     * @param channelUid the unique id of the channel
     * @param waitForKeyFrame start streaming after the first IFRAME has been received
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, String language, boolean waitForKeyFrame) {
        return openStream(channelUid, language, waitForKeyFrame, 50);
    }

    /**
     * Start streaming of a LiveTV channel
     * @param channelUid the unique id of the channel
     * @param waitForKeyFrame start streaming after the first IFRAME has been received
     * @param priority priority of the received device on the server
     * @return returns the status of the operation
     */
    public int openStream(int channelUid, String language, boolean waitForKeyFrame, int priority) {
        // stop playback
        stop();

        // open server connection
        if(!open()) {
            return ERROR;
        }

        Packet req = mConnection.CreatePacket(ServerConnection.XVDR_CHANNELSTREAM_OPEN, ServerConnection.XVDR_CHANNEL_REQUEST_RESPONSE);
        req.putU32(channelUid);
        req.putS32(priority); // priority 50
        req.putU8((short) (waitForKeyFrame ? 1 : 0)); // start with IFrame
        req.putU8((short)1); // raw PTS values
        req.putString(language);

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            return NORESPONSE;
        }

        int status = (int)resp.getU32();

        if(status > 0) {
            return ERROR;
        }

        start();

        return SUCCESS;
    }


}
