package org.xvdr.robotv.client;

import android.os.Handler;
import android.util.Log;

import org.xvdr.robotv.client.model.Movie;
import org.xvdr.recordings.model.RelatedContentExtractor;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;

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
    final private String language;

    private Collection<Movie> movieCollection = null;
    private TreeSet<String> folderList;

    public MovieController(Connection connection, String language) {
        this.language = language;
        this.connection = connection;
        this.handler = new Handler();
        this.folderList = new TreeSet<>();
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "started movie collection update");
                listener.onMovieCollectionUpdated(null, STATUS_Collection_Busy);
            }
        });

        MovieCollectionLoaderTask loaderTask = new MovieCollectionLoaderTask(connection, language);
        loaderTask.load(new MovieCollectionLoaderTask.Listener() {
            @Override
            public void onCompleted(Collection<Movie> list) {
                if(list == null) {
                    movieCollection = null;
                    listener.onMovieCollectionUpdated(null, STATUS_Collection_Error);
                    return;
                }

                Log.d(TAG, "finished loading (" + list.size() + " movies)");

                movieCollection = list;
                updateFolderList();
                Log.d(TAG, "loaded " + folderList.size() + " folders");
                listener.onMovieCollectionUpdated(movieCollection, STATUS_Collection_Ready);
            }
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
        String id = movie.getRecordingId();
        for(Movie m : movieCollection) {
            if (m.getRecordingId().equals(id)) {
                Log.d(TAG, "updating movie entry " + id);
                m.setArtwork(holder);
                break;
            }
        }

        // update on server
        ArtworkUtils.setMovieArtwork(connection, movie, holder);
    }

    public int renameMovie(Movie movie, String newName) {
        return connection.renameRecording(movie.getRecordingId(), newName);
    }

    private void updateFolderList() {
        String seriesFolder = connection.getConfig("SeriesFolder");

        for(Movie movie : movieCollection) {
            String category = movie.getFolder();

            if (!seriesFolder.isEmpty() && category.startsWith(seriesFolder + "/")) {
                continue;
            }

            if(category.isEmpty()) {
                continue;
            }

            folderList.add(movie.getFolder());
        }

        if (!seriesFolder.isEmpty()) {
            folderList.add(seriesFolder);
        }
    }

    public TreeSet<String> getFolderList() {
        return folderList;
    }

}
