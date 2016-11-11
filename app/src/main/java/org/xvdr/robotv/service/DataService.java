package org.xvdr.robotv.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.SessionListener;
import org.xvdr.recordings.model.Movie;
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

public class DataService extends Service {

    private static final String TAG = "DataService";

    public static final int STATUS_Server_Connected = 0;
    public static final int STATUS_Server_NotConnected = 1;
    public static final int STATUS_Server_Connecting = 1;

    private final IBinder binder = new Binder();

    private Connection connection;
    private HandlerThread handlerThread;
    private Handler handler;
    private Handler listenerHandler;
    private NotificationHandler notification;
    private ArtworkFetcher artwork;
    private MovieController movieController;

    private int connectionStatus = STATUS_Server_NotConnected;
    private String seriesFolder = null;

    public interface Listener {
        void onConnected(DataService service);
        void onConnectionError(DataService service);
        void onMovieUpdate(DataService service);
    }

    public class Binder extends android.os.Binder {
        DataService getService() {
            return DataService.this;
        }
    }

    SessionListener mSessionListener = new SessionListener() {
        @Override
        public void onNotification(Packet p) {
            String message;

            // process only STATUS messages
            if(p.getType() != Connection.XVDR_CHANNEL_STATUS) {
                return;
            }

            int id = p.getMsgID();

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
                    postOnMovieUpdate();
                    break;
            }
        }

        @Override
        public void onReconnect() {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "server reconnected");
                    if(connection.login()) {
                        Log.d(TAG, "logged in");
                        postOnConnected();
                        connectionStatus = STATUS_Server_Connected;
                    }
                    else {
                        Log.e(TAG, "server login failed");
                        connectionStatus = STATUS_Server_NotConnected;
                        connection.close();

                        DataService.this.postOpen();
                    }
                }
            }, 3000);
        }

        @Override
        public void onDisconnect() {
        }
    };

    private Runnable mOpenRunnable = new Runnable() {
        @Override
        public void run() {
            if(!open()) {
                notification.error(getResources().getString(R.string.failed_connect));
                handler.postDelayed(mOpenRunnable, 10 * 1000);
                return;
            }

            notification.notify(getResources().getString(R.string.service_connected));
        }
    };

    private final ArrayList<Listener> listeners = new ArrayList<>();

    public DataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service");

        // check if the server has changed
        String server = SetupUtils.getServer(this);

        if(connection != null && connectionStatus == STATUS_Server_NotConnected) {
            postOpen();
        }
        else if(connection != null && !connection.getHostname().equals(server)) {
            Log.i(TAG, "new server: " + server);
            connection.close();
            postOpen();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        handlerThread = new HandlerThread("dataservice");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        listenerHandler = new Handler();

        notification = new NotificationHandler(this);

        connection = new Connection(
            "roboTV:dataservice",
            SetupUtils.getLanguage(this),
            true);

        connection.setCallback(mSessionListener);

        artwork = new ArtworkFetcher(connection, SetupUtils.getLanguage(this));
        movieController = new MovieController(this, connection);
    }

    @Override
    public void onDestroy() {
        connectionStatus = STATUS_Server_NotConnected;
        connection.close();
        handlerThread.interrupt();
    }

    private boolean open() {
        if(connectionStatus == STATUS_Server_Connected) {
            postOnConnected();
            return true;
        }

        connectionStatus = STATUS_Server_Connecting;
        connection.setPriority(1); // low priority for DataService
        if(!connection.open(SetupUtils.getServer(this))) {
            connection.close();
            connectionStatus = STATUS_Server_NotConnected;
            postOnConnectionError();

            return false;
        }

        connectionStatus = STATUS_Server_Connected;
        postOnConnected();

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

    public String getSeriesFolder() {
        if(seriesFolder != null) {
            return seriesFolder;
        }

        seriesFolder = connection.getConfig("SeriesFolder");
        return seriesFolder;
    }

    public boolean createTimer(Movie movie) {
        String name;
        Timer timer = new Timer(connection);

        String category = movie.getFolder();

        if(category.equals(getSeriesFolder())) {
            name = getSeriesFolder() + "~" + movie.getTitle() + "~" + movie.getShortText();
        }
        else {
            name = category;

            if (!name.isEmpty()) {
                name += "~";
            }

            name += movie.getTitle();
        }

        return timer.create(movie.getChannelUid(), movie.getStartTime(), movie.getDuration(), name);
    }

    public Connection getConnection() {
        return connection;
    }

    public void registerListener(Listener listener) {
        listeners.add(listener);

        if(connectionStatus == STATUS_Server_Connected) {
            postOnConnected(listener);
        }
    }

    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public MovieController getMovieController() {
        return movieController;
    }

    // post open request

    private void postOpen() {
        Log.d(TAG, "postOpen");
        handler.removeCallbacks(mOpenRunnable);
        handler.post(mOpenRunnable);
    }

    // post events

    private void postOnConnected(final Listener listener) {
        if(listener == null) {
            return;
        }

        listenerHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onConnected(DataService.this);
            }
        });
    }

    private void postOnConnected() {
        Log.d(TAG, "postOnConnected");
        listenerHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onConnected(DataService.this);
                }
            }
        });
    }

    private void postOnConnectionError() {
        Log.d(TAG, "postOnConnectionError");
        listenerHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onConnectionError(DataService.this);
                }
            }
        });
    }

    private void postOnMovieUpdate() {
        listenerHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onMovieUpdate(DataService.this);
                }
            }
        });
    }
}
