package org.xvdr.recordings.model;

import android.os.AsyncTask;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;
import org.xvdr.robotv.client.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class MovieCollectionLoaderTask extends AsyncTask<Connection, Void, Collection> {

    public interface Listener {
        void onStart();
        void onCompleted(Collection<Movie> adapter);
    }

    private final static String TAG = "MovieCollectionTask";

    private Connection connection;
    private Listener listener;
    private ArtworkFetcher fetcher;

    public MovieCollectionLoaderTask(Connection connection, String language) {
        this.connection = connection;
        this.fetcher = new ArtworkFetcher(this.connection, language);
    }

    public void load(Listener listener) {
        this.listener = listener;
        execute(connection);
    }

    @Override
    protected void onPreExecute() {
        if(listener != null) {
            listener.onStart();
        }
    }

    @Override
    protected Collection doInBackground(Connection... params) {

        // get movies
        Packet request = connection.CreatePacket(Connection.XVDR_RECORDINGS_GETLIST);
        Packet response = connection.transmitMessage(request);

        if(response == null) {
            Log.e(TAG, "no response for request");
            return null;
        }

        ArrayList<Movie> collection = new ArrayList<>(500);

        // uncompress response
        response.uncompress();

        while(!response.eop()) {
            final Movie movie = PacketAdapter.toMovie(response);
            collection.add(movie);

            // search movies
            String url = movie.getCardImageUrl();

            if(url != null && !url.isEmpty() && !url.equals("x")) {
                Log.d(TAG, "recording '" + movie.getTitle() + "' has url: '" + movie.getCardImageUrl() + "'");
            }
            else {
                updateMovieArtwork(movie);
            }
        }

        return collection;
    }

    @Override
    protected void onPostExecute(Collection result) {
        if(listener != null) {
            listener.onCompleted(result);
        }
    }

    private void updateMovieArtwork(final Movie searchMovie) {
        ArtworkHolder o = null;

        try {
            o = fetcher.fetchForEvent(searchMovie.getEvent());
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        if(o == null) {
            return;
        }

        searchMovie.setCardImageUrl(o.getPosterUrl());
        searchMovie.setBackgroundImageUrl(o.getBackgroundUrl());

        // send new urls to server
        ArtworkUtils.setMovieArtwork(connection, searchMovie);
    }
}