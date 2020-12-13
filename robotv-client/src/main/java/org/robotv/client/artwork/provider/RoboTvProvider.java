package org.robotv.client.artwork.provider;

import org.robotv.msgexchange.Packet;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.model.Event;
import org.robotv.client.Connection;

public class RoboTvProvider extends SimpleArtworkProvider {

    private final Connection mConnection;

    public RoboTvProvider(Connection connection) {
        mConnection = connection;
    }

    @Override
    public ArtworkHolder search(Event event) {

        // request artwork from server cache

        Packet req = mConnection.CreatePacket(Connection.ARTWORK_GET);
        req.putString(event.getTitle());
        req.putU32(event.getContentId());

        // update EPG entry
        req.putU32(event.getChannelUid());
        req.putU32(event.getEventId());

        Packet resp = mConnection.transmitMessage(req);

        if(resp == null) {
            return null;
        }

        String posterUrl = resp.getString();
        String backgroundUrl = resp.getString();

        if(posterUrl.equals("x") || backgroundUrl.equals("x")) {
            return null;
        }

        return new ArtworkHolder(posterUrl, backgroundUrl);
    }
}
