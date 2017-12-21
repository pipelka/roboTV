package org.xvdr.robotv.artwork;

import org.robotv.msgexchange.Packet;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.Connection;

public class ArtworkUtils {

    public static boolean setMovieArtwork(Connection connection, Movie movie, ArtworkHolder holder) {
        movie.setPosterUrl(holder.getPosterUrl());
        movie.setBackgroundUrl(holder.getBackgroundUrl());

        return setMovieArtwork(connection, movie);
    }

    public static boolean setMovieArtwork(Connection connection, Movie movie) {
        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_SETURLS);

        p.putString(movie.getRecordingIdString());
        p.putString(movie.getPosterUrl());
        p.putString(movie.getBackgroundUrl());
        p.putU32(0);

        return (connection.transmitMessage(p) != null);
    }

}
