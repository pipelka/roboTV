package org.robotv.client;

import org.robotv.msgexchange.Packet;
import org.robotv.client.model.Channel;

import java.util.ArrayList;

public class Channels extends ArrayList<Channel> {

    public interface Callback {
        boolean onChannel(Channel entry);
    }

    public void load(Connection connection) {
        load(connection, "");
    }

    public void load(Connection connection, String language) {
        clear();

        loadChannelType(connection, false, language, null);
    }

    public void load(Connection connection, String language, Callback callback) {
        clear();

        loadChannelType(connection, false, language, callback);
    }

    public Channel findByUid(int uid) {
        for(Channel e : this) {
            if(e.getUid() == uid) {
                return e;
            }
        }

        return null;
    }

    private boolean loadChannelType(Connection connection, boolean radio, String language, Callback callback) {
        Packet req = connection.CreatePacket(Connection.CHANNELS_GETCHANNELS);
        req.putU32(radio ? 1 : 0);
        req.putString(language);
        req.putU32(1);
        req.putU32(0);

        Packet resp = connection.transmitMessage(req);

        if(resp == null) {
            return false;
        }

        while(!resp.eop()) {
            Channel c = PacketAdapter.toChannel(resp);
            c.setRadio(radio);

            if(callback != null) {
                if(!callback.onChannel(c)) {
                    return false;
                }
            }
            else {
                add(c);
            }
        }

        return true;
    }
}
