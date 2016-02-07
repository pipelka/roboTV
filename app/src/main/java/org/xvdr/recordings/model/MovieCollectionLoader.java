package org.xvdr.recordings.model;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.util.Log;

import org.json.JSONArray;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tmdb.TheMovieDatabase;
import org.xvdr.robotv.tv.ServerConnection;

import java.util.ArrayList;
import java.util.List;

public class MovieCollectionLoader extends AsyncTask<ServerConnection, Void, MovieCollectionAdapter> {

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
    private final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    private ServerConnection mConnection;
    private Listener mListener;
    private TheMovieDatabase mMovieDb;
    private List<ArtworkItem> mArtworkQueue = new ArrayList<>();

    public MovieCollectionLoader(ServerConnection connection) {
        this(connection, "");
    }

    public MovieCollectionLoader(ServerConnection connection, String language) {
        mConnection = connection;
        mMovieDb = new TheMovieDatabase(TMDB_APIKEY, language);
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
    protected MovieCollectionAdapter doInBackground(ServerConnection... params) {
        final MovieCollectionAdapter collection = new MovieCollectionAdapter();

        Packet request = mConnection.CreatePacket(ServerConnection.XVDR_RECORDINGS_GETLIST);
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
            int genreType = movie.getGenreType();

            if(url != null) {
                Log.d(TAG, "recording '" + movie.getTitle() + "' has url: '" + movie.getCardImageUrl() + "'");
            }

            if((genreType == 0x10 || genreType == 0x00) && (url == null || url.isEmpty())) {
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
                    JSONArray o = mMovieDb.searchMovie(item.searchMovie.getTitle());
                    if (o == null) {
                        continue;
                    }

                    String url = mMovieDb.getPosterUrl(o);
                    item.searchMovie.setCardImageUrl(url);
                    item.searchMovie.setBackgroundImageUrl(mMovieDb.getBackgroundUrl(o));

                    // update item
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int index = item.rowAdapter.indexOf(item.searchMovie);
                            item.rowAdapter.notifyArrayItemRangeChanged(index, 1);
                        }
                    });

                    // send new urls to server
                    Packet p = mConnection.CreatePacket(ServerConnection.XVDR_RECORDINGS_SETURLS);
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
