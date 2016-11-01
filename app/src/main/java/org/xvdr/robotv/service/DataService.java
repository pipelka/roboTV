package org.xvdr.robotv.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.SessionListener;
import org.xvdr.recordings.model.RelatedContentExtractor;
import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.model.MovieCollectionLoaderTask;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.Timer;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

public class DataService extends Service implements MovieCollectionLoaderTask.Listener {

    public static final int STATUS_Collection_Busy = 0;
    public static final int STATUS_Collection_Ready = 1;

    public static final int STATUS_Server_Connected = 0;
    public static final int STATUS_Server_Failed = 1;
    public static final int STATUS_Server_Starting = 2;

    private final IBinder binder = new Binder();

    Connection connection;
    Handler handler;
    NotificationHandler notification;
    ArtworkFetcher artwork;
    Collection<Movie> movieCollection;
    TreeSet<String> folderList;
    int connectionStatus = STATUS_Server_Starting;
    private String seriesFolder = null;

    private MovieCollectionLoaderTask loaderTask;

    private static final String TAG = "DataService";

    public interface Listener {
        void onMovieCollectionUpdated(Collection<Movie> collection, int status);
    }

    public class Binder extends android.os.Binder {
        DataService getService() {
            return DataService.this;
        }
    }

    SessionListener mSessionListener = new SessionListener() {
        public void onNotification(Packet p) {
            String message;

            // process only STATUS messages
            if(p.getType() != Connection.XVDR_CHANNEL_STATUS) {
                return;
            }

            int id = p.getMsgID();
            Log.d(TAG, "notification id: " + id);

            switch(id) {
                case Connection.XVDR_STATUS_MESSAGE:
                    p.getU32(); // type
                    message = p.getString();
                    notification.notify(message);
                    break;

                case Connection.XVDR_STATUS_RECORDING:
                    onRecording(p);
                    break;

                case Connection.XVDR_STATUS_RECORDINGSCHANGE:
                    loadMovieCollection();
                    break;
            }
        }

        public void onDisconnect() {
            connectionStatus = STATUS_Server_Failed;
        }

        public void onReconnect() {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "server reconnected");
                    if(connection.login()) {
                        Log.d(TAG, "logged in");
                        loadMovieCollection();
                        connectionStatus = STATUS_Server_Connected;
                    }
                    else {
                        Log.e(TAG, "server login failed");
                    }
                }
            }, 3000);
        }
    };

    private Runnable mOpenRunnable = new Runnable() {
        @Override
        public void run() {
            if(!open()) {
                handler.postDelayed(mOpenRunnable, 2 * 1000);
                return;
            }

            notification.notify(getResources().getString(R.string.service_connected));
        }
    };

    private final ArrayList<Listener> listeners = new ArrayList<>();

    public DataService() {
        movieCollection = new ArrayList<>(500);
        folderList = new TreeSet<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service");

        // check if the server has changed
        String server = SetupUtils.getServer(this);

        if(connection != null && !connection.getHostname().equals(server)) {
            Log.i(TAG, "new server: " + server);
            connection.close();
            handler.removeCallbacks(mOpenRunnable);
            handler.post(mOpenRunnable);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        notification = new NotificationHandler(this);

        connection = new Connection(
            "roboTV:dataservice",
            SetupUtils.getLanguage(this),
            true);

        connection.setCallback(mSessionListener);

        artwork = new ArtworkFetcher(connection, SetupUtils.getLanguage(this));

        handler.post(mOpenRunnable);
    }

    @Override
    public void onDestroy() {
        connection.close();
    }

    private boolean open() {
        if(connection.isOpen()) {
            connectionStatus = STATUS_Server_Connected;
            return true;
        }

        connection.setPriority(1); // low priority for DataService
        if(!connection.open(SetupUtils.getServer(this))) {
            connectionStatus = STATUS_Server_Failed;
            return false;
        }

        connectionStatus = STATUS_Server_Connected;

        // movie collection loader
        loadMovieCollection();

        return true;
    }

    private void onRecording(Packet p) {
        p.getU32(); // recording index - currently unused
        boolean on = (p.getU32() == 1); // on
        String title = p.getString(); // title
        p.getString(); // description - currently unused

        final String message = getResources().getString(R.string.recording_text) + " " + (on ?
                               getResources().getString(R.string.recording_started) :
                               getResources().getString(R.string.recording_finished));

        // we do not have an event attached

        if(p.eop()) {
            notification.notify(message, title, R.drawable.ic_movie_white_48dp);
            return;
        }

        // process attached event

        final Event event = ArtworkUtils.packetToEvent(p);

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ArtworkHolder holder = artwork.fetchForEvent(event);

                    if(holder != null) {
                        notification.notify(message, event.getTitle(), holder.getBackgroundUrl());
                    }
                    else {
                        notification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                    notification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
                }
            }
        });
    }

    public void setMovieArtwork(Movie movie, ArtworkHolder holder) {
        if(movie.isSeries() || movie.isSeriesHeader()) {
            Collection<Movie> episodes = new RelatedContentExtractor(movieCollection).getSeries(movie.getTitle());
            for(Movie m: episodes) {
                setArtwork(m, holder);
            }
        }
        else {
            setArtwork(movie, holder);
        }

        postMovieCollectionUpdated(movieCollection);
    }

    private void setArtwork(Movie movie, ArtworkHolder holder) {
        // update local movie list
        String id = movie.getId();
        for(Movie m : movieCollection) {
            if (m.getId().equals(id)) {
                Log.d(TAG, "updating movie entry " + id);
                m.setArtwork(holder);
                break;
            }
        }

        // update on server
        ArtworkUtils.setMovieArtwork(connection, movie, holder);
    }

    public Collection<Movie> getMovieCollection() {
        return movieCollection;
    }

    public TreeSet<String> getFolderList() {
        return folderList;
    }

    public String getSeriesFolder() {
        if(seriesFolder != null) {
            return seriesFolder;
        }

        seriesFolder = connection.getConfig("SeriesFolder");
        return seriesFolder;
    }

    protected void loadMovieCollection() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "started movie collection update");

                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onMovieCollectionUpdated(null, STATUS_Collection_Busy);
                }

                loaderTask = new MovieCollectionLoaderTask(connection, SetupUtils.getLanguage(DataService.this));
                loaderTask.load(DataService.this);
            }
        });
    }

    public int deleteMovie(Movie movie) {
        return connection.deleteRecording(movie.getId());
    }

    public int renameMovie(Movie movie, String newName) {
        return connection.renameRecording(movie.getId(), newName);
    }

    protected void updateFolderList() {
        String seriesFolder = connection.getConfig("SeriesFolder");

        for(Movie movie : movieCollection) {
            String category = movie.getCategory();

            if (!seriesFolder.isEmpty() && category.startsWith(seriesFolder + "/")) {
                continue;
            }

            if(category.isEmpty()) {
                continue;
            }

            folderList.add(movie.getCategory());
        }

        if (!seriesFolder.isEmpty()) {
            folderList.add(seriesFolder);
        }
    }

    public boolean createTimer(Movie movie) {
        String name;
        Timer timer = new Timer(connection);

        String category = movie.getCategory();

        if(category.equals(getSeriesFolder())) {
            name = getSeriesFolder() + "~" + movie.getTitle() + "~" + movie.getOutline();
        }
        else {
            name = category;

            if (!name.isEmpty()) {
                name += "~";
            }

            name += movie.getTitle();
        }

        return timer.create(movie.getChannelUid(), movie.getStartTime(), (int)movie.getDuration(), name);
    }

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public Collection<Movie> getRelatedContent(Movie movie) {
        RelatedContentExtractor contentExtractor = new RelatedContentExtractor(movieCollection);
        if(movie.isSeries()) {
            return contentExtractor.getSeries(movie.getTitle());
        }

        return contentExtractor.getRelatedMovies(movie);
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    // MovieCollectionLoaderTask

    @Override
    public void onStart() {
        Log.d(TAG, "started loading movies");
    }

    @Override
    public void onCompleted(Collection<Movie> list) {
        if(list == null) {
            postMovieCollectionUpdated(null);
            return;
        }

        Log.d(TAG, "finished loading (" + list.size() + " movies)");

        movieCollection = list;
        updateFolderList();
        Log.d(TAG, "loaded " + folderList.size() + " folders");
        postMovieCollectionUpdated(movieCollection);
    }

    private void postMovieCollectionUpdated(final Collection<Movie> list) {
        handler.post(new Runnable() {
            @Override
            public void run() {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onMovieCollectionUpdated(list, STATUS_Collection_Ready);
            }
            }
        });
    }
}
