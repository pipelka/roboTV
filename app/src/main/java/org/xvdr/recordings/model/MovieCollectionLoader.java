package org.xvdr.recordings.model;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.tmdb.TheMovieDatabase;
import org.xvdr.robotv.tv.ServerConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MovieCollectionLoader extends AsyncTask<ServerConnection, Void, MovieCollectionAdapter> {

    public interface Listener {
        void onStart();
        void onCompleted(MovieCollectionAdapter adapter);
    }

    private final static String TAG = "MovieCollectionLoader";
    private final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    private ServerConnection mConnection;
    private Listener mListener;
    private ThreadPoolExecutor mThreadPool;
    private BlockingQueue<Runnable> mWorkQueue;
    private Handler mHandler;
    private TheMovieDatabase mMovieDb;

    public MovieCollectionLoader(ServerConnection connection) {
        this(connection, "");
    }

    public MovieCollectionLoader(ServerConnection connection, String language) {
        mConnection = connection;

        mWorkQueue = new ArrayBlockingQueue<Runnable>(10000);
        mThreadPool = new ThreadPoolExecutor(1, 1, 10, TimeUnit.MINUTES, mWorkQueue);
        mHandler = new Handler();
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
        MovieCollectionAdapter collection = new MovieCollectionAdapter();

        Packet request = mConnection.CreatePacket(ServerConnection.XVDR_RECORDINGS_GETLIST);
        Packet response = mConnection.transmitMessage(request);

        if(response == null) {
            Log.e(TAG, "no response for request");
            mConnection.close();
            return null;
        }

        int i = 0;
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
                Log.d(TAG, "trying to fetch artwork for: " + movie.getTitle());

                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                    updateMovieArtwork(movie, adapter);
                    }
                });
            }
        }

        return collection;
    }

    @Override
    protected void onPostExecute(MovieCollectionAdapter result) {
        if(mListener != null) {
            mListener.onCompleted(result);
        }
    }

    protected void updateMovieArtwork(Movie searchMovie, final ArrayObjectAdapter rowAdapter) {
        JSONObject o = mMovieDb.searchMovie(searchMovie.getTitle());
        if (o != null) {
            try {
                String url = mMovieDb.getPosterUrl(o);
                searchMovie.setCardImageUrl(url);
                searchMovie.setBackgroundImageUrl(mMovieDb.getBackgroundUrl(o));

                // update item
                final int index = rowAdapter.indexOf(searchMovie);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        rowAdapter.notifyArrayItemRangeChanged(index, 1);
                    }
                });

                // send new urls to server
                Packet p = mConnection.CreatePacket(ServerConnection.XVDR_RECORDINGS_SETURLS);
                p.putString(searchMovie.getId());
                p.putString(searchMovie.getCardImageUrl());
                p.putString(searchMovie.getBackgroundImageUrl());
                p.putU32(0);

                Packet response = mConnection.transmitMessage(p);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
