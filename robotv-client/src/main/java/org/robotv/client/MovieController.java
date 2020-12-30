package org.robotv.client;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.robotv.client.model.Event;
import org.robotv.client.model.Movie;
import org.robotv.msgexchange.Packet;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.ArtworkUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class MovieController {

    private static final String TAG = "MovieController";

    public static final int STATUS_Collection_Busy = 0;
    public static final int STATUS_Collection_Ready = 1;
    public static final int STATUS_Collection_Error = 2;

    public interface LoaderCallback {
        void onMovieCollectionUpdated(ArrayList<Movie> collection, int status);
    }

    final private Connection connection;
    final private Handler handler;

    private ArrayList<Movie> movieCollection = null;

    static public Comparator<Event> compareTimestamps = (lhs, rhs) -> {
        if(lhs.getStartTime() == rhs.getStartTime()) {
            return 0;
        }

        return lhs.getStartTime() > rhs.getStartTime() ? -1 : 1;
    };

    static public final Comparator<Event> compareTimestampsReverse = (lhs, rhs) -> {
        if(lhs.getStartTime() == rhs.getStartTime()) {
            return 0;
        }

        return lhs.getStartTime() < rhs.getStartTime() ? -1 : 1;
    };


    public MovieController(Connection connection) {
        this.connection = connection;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public int deleteMovie(Movie movie) {
        return connection.deleteRecording(movie.getRecordingId());
    }

    public ArrayList<Movie> getRelatedContent(Movie movie) {
        RelatedContentExtractor contentExtractor = new RelatedContentExtractor(movieCollection);
        if(movie.isTvShow()) {
            return contentExtractor.getSeries(movie.getTitle());
        }

        return contentExtractor.getRelatedMovies(movie);
    }

    public void loadMovieCollection(final LoaderCallback listener) {
        handler.post(() -> {
            Log.i(TAG, "started movie collection update");
            listener.onMovieCollectionUpdated(null, STATUS_Collection_Busy);
        });

        MovieCollectionLoaderTask loaderTask = new MovieCollectionLoaderTask(connection);
        loaderTask.load(list -> {
            if(list == null) {
                movieCollection = null;
                handler.post(() -> listener.onMovieCollectionUpdated(null, STATUS_Collection_Error));
                return;
            }

            Log.d(TAG, "finished loading (" + list.size() + " movies)");

            movieCollection = list;
            handler.post(() -> listener.onMovieCollectionUpdated(movieCollection, STATUS_Collection_Ready));
        });
    }

    public ArrayList<Movie> load() {
        MovieCollectionLoaderTask loaderTask = new MovieCollectionLoaderTask(connection);
        return loaderTask.loadSync();
    }

    public ArrayList<Movie> getMovieCollection() {
        return movieCollection;
    }

    public void setMovieArtwork(Movie movie, ArtworkHolder holder) {
        if(movie.isTvShow() || movie.isSeriesHeader()) {
            Collection<Movie> episodes = new RelatedContentExtractor(movieCollection).getSeries(movie.getTitle());
            for(Movie m: episodes) {
                setArtwork(m, holder);
            }
        }
        else {
            setArtwork(movie, holder);
        }
    }

    private void setArtwork(Movie movie, ArtworkHolder holder) {
        // update local movie list
        int id = movie.getRecordingId();
        for(Movie m : movieCollection) {
            if (m.getRecordingId() == id) {
                Log.d(TAG, "updating movie entry " + id);
                if(holder != null) {
                    m.setArtwork(holder.getPosterUrl(), holder.getBackgroundUrl());
                }
                break;
            }
        }

        // update on server
        if(holder != null) {
            ArtworkUtils.setMovieArtwork(connection, movie, holder);
        }
    }

    public void renameMovie(Movie movie, String newName) {
        connection.renameRecording(movie.getRecordingId(), newName);
    }

    public TreeSet<String> getFolderList() {
        TreeSet<String> folderList = new TreeSet<>();

        Packet p = connection.CreatePacket(Connection.RECORDINGS_GETFOLDERS);
        Packet r = connection.transmitMessage(p);

        if(r == null) {
            return folderList;
        }

        r.uncompress();

        while(!r.eop()) {
            folderList.add(r.getString());
        }

        return folderList;
    }

    public void setPlaybackPosition(Movie movie, long lastPosition) {
        Packet p = connection.CreatePacket(Connection.RECORDINGS_SETPOSITION);

        p.putString(movie.getRecordingIdString());
        p.putU64(BigInteger.valueOf(lastPosition));

        connection.transmitMessage(p);
    }

    public long getPlaybackPosition(String recid) {
        Packet p = connection.CreatePacket(Connection.RECORDINGS_GETPOSITION);
        p.putString(recid);

        Packet r = connection.transmitMessage(p);

        if(r == null || r.eop()) {
            return 0;
        }

        return r.getU64().longValue();
    }

    public long getPlaybackPosition(Movie movie) {
        return getPlaybackPosition(movie.getRecordingIdString());
    }

    public Movie getMovie(String recid) {
        Packet p = connection.CreatePacket(Connection.RECORDINGS_GETMOVIE);
        p.putString(recid);

        Packet r = connection.transmitMessage(p);

        if(r == null || r.eop()) {
            return null;
        }

        return PacketAdapter.toMovie(r);
    }

}
