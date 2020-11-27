package org.robotv.client.artwork;

import android.util.Log;

import org.robotv.msgexchange.Packet;
import org.robotv.client.model.Movie;
import org.robotv.client.Connection;

public class ArtworkUtils {

    final private static String TAG = ArtworkUtils.class.getName();

    public static void setMovieArtwork(Connection connection, Movie movie, ArtworkHolder holder) {
        movie.setPosterUrl(holder.getPosterUrl());
        movie.setBackgroundUrl(holder.getBackgroundUrl());

        setMovieArtwork(connection, movie);
    }

    private static void setMovieArtwork(Connection connection, Movie movie) {
        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_SETURLS);

        p.putString(movie.getRecordingIdString());
        p.putString(movie.getPosterUrl());
        p.putString(movie.getBackgroundUrl());
        p.putU32(0);

        Packet r = connection.transmitMessage(p);

        if(r == null) {
            Log.d(TAG, "failed to update move artwork");
        }
    }

}
