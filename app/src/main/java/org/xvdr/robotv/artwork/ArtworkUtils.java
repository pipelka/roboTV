package org.xvdr.robotv.artwork;

import org.xvdr.jniwrap.Packet;
import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.client.Connection;

import java.util.ArrayList;
import java.util.List;

public class ArtworkUtils {

    public static boolean setMovieArtwork(Connection connection, Movie movie, ArtworkHolder holder) {
        movie.setCardImageUrl(holder.getPosterUrl());
        movie.setBackgroundImageUrl(holder.getBackgroundUrl());

        return setMovieArtwork(connection, movie);
    }

    public static boolean setMovieArtwork(Connection connection, Movie movie) {
        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_SETURLS);

        p.putString(movie.getId());
        p.putString(movie.getCardImageUrl());
        p.putString(movie.getBackgroundImageUrl());
        p.putU32(0);

        return (connection.transmitMessage(p) != null);
    }

    public static Event packetToEvent(Packet p) {
        final int eventId = (int) p.getU32();
        long startTime = p.getU32();
        final int duration = (int) p.getU32();
        int contentId = 0;
        List<Integer> list = new ArrayList<>();

        while((contentId = (int) p.getU8()) != 0) {
            list.add(contentId);
        }

        if(list.size() != 0) {
            contentId = list.get(0);
        }

        int rating = (int) p.getU32();
        String title = p.getString();
        String shortText = p.getString();
        String description = p.getString();

        Event e = new Event(contentId, title, shortText, description, duration, eventId);
        e.setStartTime(startTime);

        return e;
    }
}
