package org.robotv.client;

import android.os.Handler;
import android.util.Log;

import org.robotv.client.model.Movie;
import org.robotv.msgexchange.Packet;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.ArtworkUtils;

import java.math.BigInteger;
import java.util.Collection;
import java.util.TreeSet;

public class MovieController {

    private static final String TAG = "MovieController";

    public static final int STATUS_Collection_Busy = 0;
    public static final int STATUS_Collection_Ready = 1;
    public static final int STATUS_Collection_Error = 2;

    public interface LoaderCallback {
        void onMovieCollectionUpdated(Collection<Movie> collection, int status);
    }

    final private Connection connection;
    final private Handler handler;

    private Collection<Movie> movieCollection = null;

    public MovieController(Connection connection) {
        this.connection = connection;
        this.handler = new Handler();
    }

    public int deleteMovie(Movie movie) {
        return connection.deleteRecording(movie.getRecordingId());
    }

    public Collection<Movie> getRelatedContent(Movie movie) {
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
                listener.onMovieCollectionUpdated(null, STATUS_Collection_Error);
                return;
            }

            Log.d(TAG, "finished loading (" + list.size() + " movies)");

            movieCollection = list;
            listener.onMovieCollectionUpdated(movieCollection, STATUS_Collection_Ready);
        });
    }

    public Collection<Movie> getMovieCollection() {
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

        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_GETFOLDERS);
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
        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_SETPOSITION);

        p.putString(movie.getRecordingIdString());
        p.putU64(BigInteger.valueOf(lastPosition));

        connection.transmitMessage(p);
    }

    public long getPlaybackPosition(Movie movie) {
        Packet p = connection.CreatePacket(Connection.XVDR_RECORDINGS_GETPOSITION);
        p.putString(movie.getRecordingIdString());

        Packet r = connection.transmitMessage(p);

        if(r == null || r.eop()) {
            return 0;
        }

        return r.getU64().longValue();
    }

}
