package org.xvdr.robotv.artwork.provider;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.model.Event;
import org.xvdr.robotv.client.Connection;

public class RoboTvProvider extends SimpleArtworkProvider {

    private Connection mConnection;

    public RoboTvProvider(Connection connection) {
        mConnection = connection;
    }

    @Override
    public ArtworkHolder search(Event event) {

        // request artwork from server cache

        Packet req = mConnection.CreatePacket(Connection.XVDR_ARTWORK_GET);
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
