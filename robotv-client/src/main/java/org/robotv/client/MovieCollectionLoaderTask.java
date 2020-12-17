package org.robotv.client;

import android.util.Log;

import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.msgexchange.Packet;
import org.robotv.client.model.Movie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MovieCollectionLoaderTask {

    private final static String TAG = MovieCollectionLoaderTask.class.getName();

    public interface Listener {
        void onCompleted(ArrayList<Movie> adapter);
    }

    private final static Executor executor = Executors.newSingleThreadExecutor();

    private final Connection connection;
    private final ArtworkFetcher artwork;

    MovieCollectionLoaderTask(Connection connection) {
        this.connection = connection;
        this.artwork = new ArtworkFetcher(connection, "de");
    }

    public void load(Listener listener) {
        executor.execute(() -> {
            ArrayList<Movie> list = loadSync();

            if(list == null) {
                listener.onCompleted(null);
            }

            for(Movie movie : list) {
                try {
                    artwork.fetchForEvent(movie);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            listener.onCompleted(list);
        });
    }

    public ArrayList<Movie> loadSync() {
        // get movies
        Packet request = connection.CreatePacket(Connection.RECORDINGS_GETLIST);
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
        }

        return collection;
    }
}
