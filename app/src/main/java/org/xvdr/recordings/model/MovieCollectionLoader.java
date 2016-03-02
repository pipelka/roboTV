package org.xvdr.recordings.model;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.util.Log;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MovieCollectionLoader extends AsyncTask<Connection, Void, MovieCollectionAdapter> {

    public interface Listener {
        void onStart();
        void onCompleted(MovieCollectionAdapter adapter);
    }

    protected static class ArtworkItem {
        ArtworkItem(Movie searchMovie, ArrayObjectAdapter rowAdapter) {
            this.searchMovie = searchMovie;
            this.rowAdapter = rowAdapter;
        }

        final Movie searchMovie;
        final ArrayObjectAdapter rowAdapter;
    }

    private final static String TAG = "MovieCollectionLoader";

    private Connection mConnection;
    private Listener mListener;
    private ArtworkFetcher mFetcher;
    private List<ArtworkItem> mArtworkQueue = new ArrayList<>();

    public MovieCollectionLoader(Connection connection) {
        this(connection, "");
    }

    public MovieCollectionLoader(Connection connection, String language) {
        mConnection = connection;
        mFetcher = new ArtworkFetcher(mConnection, language);
    }

    public void load(Listener listener) {
        mListener = listener;
        execute(mConnection);
    }

    @Override
    protected void onPreExecute() {
        if(mListener != null) {
            mListener.onStart();
        }
    }

    @Override
    protected MovieCollectionAdapter doInBackground(Connection... params) {
        final MovieCollectionAdapter collection = new MovieCollectionAdapter();

        Packet request = mConnection.CreatePacket(Connection.XVDR_RECORDINGS_GETLIST);
        Packet response = mConnection.transmitMessage(request);

        if(response == null) {
            Log.e(TAG, "no response for request");
            mConnection.close();
            return null;
        }

        while(!response.eop()) {
            final Movie movie = PacketAdapter.toMovie(response);
            final ArrayObjectAdapter adapter = collection.add(movie);

            // search movies
            String url = movie.getCardImageUrl();

            if(url != null && !url.isEmpty() && !url.equals("x")) {
                Log.d(TAG, "recording '" + movie.getTitle() + "' has url: '" + movie.getCardImageUrl() + "'");
            }
            else {
                updateMovieArtwork(movie, adapter);
            }
        }

        return collection;
    }

    @Override
    protected void onPostExecute(MovieCollectionAdapter result) {
        // start update thread
        Thread thread = new Thread(new Runnable() {
            Handler mHandler = new Handler();

            @Override
            public void run() {
                for(final ArtworkItem item : mArtworkQueue) {
                    ArtworkHolder o = null;

                    try {
                        o = mFetcher.fetchForEvent(item.searchMovie.getEvent());
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(o == null) {
                        continue;
                    }

                    item.searchMovie.setCardImageUrl(o.getPosterUrl());
                    item.searchMovie.setBackgroundImageUrl(o.getBackgroundUrl());

                    // update item
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int index = item.rowAdapter.indexOf(item.searchMovie);
                            item.rowAdapter.notifyArrayItemRangeChanged(index, 1);
                        }
                    });

                    // send new urls to server
                    Packet p = mConnection.CreatePacket(Connection.XVDR_RECORDINGS_SETURLS);
                    p.putString(item.searchMovie.getId());
                    p.putString(item.searchMovie.getCardImageUrl());
                    p.putString(item.searchMovie.getBackgroundImageUrl());
                    p.putU32(0);

                    mConnection.transmitMessage(p);
                }
            }
        });

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

        if(mListener != null) {
            mListener.onCompleted(result);
        }
    }

    protected void updateMovieArtwork(final Movie searchMovie, final ArrayObjectAdapter rowAdapter) {
        mArtworkQueue.add(new ArtworkItem(searchMovie, rowAdapter));
    }
}
