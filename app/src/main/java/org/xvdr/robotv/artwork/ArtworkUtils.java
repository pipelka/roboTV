package org.xvdr.robotv.artwork;

import org.xvdr.msgexchange.Packet;
import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.client.Connection;

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

}
