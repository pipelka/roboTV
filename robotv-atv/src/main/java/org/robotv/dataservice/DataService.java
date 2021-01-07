package org.robotv.dataservice;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import org.robotv.msgexchange.Packet;
import org.robotv.msgexchange.SessionListener;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.PacketAdapter;
import org.robotv.client.model.Event;
import org.robotv.client.Connection;
import org.robotv.client.MovieController;
import org.robotv.client.TimerController;
import org.robotv.client.model.Timer;
import org.robotv.recordings.homescreen.RoboTVChannel;
import org.robotv.robotv.R;
import org.robotv.setup.SetupActivity;
import org.robotv.setup.SetupUtils;
import org.robotv.sync.SyncJobService;

import java.io.IOException;

public class DataService extends Service {

    private static final String TAG = DataService.class.getName();
    private static final int SYNC_JOB_ID = 1111;

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
    private TimerController timerController;

    private RoboTVChannel channel;

    private int connectionStatus = STATUS_Server_NotConnected;
    private String seriesFolder = null;
    private boolean wasConnected = false;

    public interface Listener {
        void onConnected(DataService service);
        void onConnectionError(DataService service);
        void onMovieUpdate(DataService service);
        void onTimersUpdated(DataService service);
    }

    class Binder extends android.os.Binder {
        DataService getService() {
            return DataService.this;
        }
    }

    SessionListener mSessionListener = new SessionListener() {
        @Override
        public void onNotification(Packet p) {
            String message;

            // process only STATUS messages
            if(p.getType() != Connection.CHANNEL_STATUS) {
                return;
            }

            int id = p.getMsgID();

            switch(id) {
                case Connection.STATUS_MESSAGE:
                    p.getU32(); // type
                    message = p.getString();
                    notification.notify(message);
                    break;

                case Connection.STATUS_RECORDING:
                    onRecording(p);
                    break;

                case Connection.STATUS_RECORDINGSCHANGE:
                    postOnMovieUpdate();
                    break;

                case Connection.STATUS_TIMERCHANGE:
                    if(!p.eop()) {
                        Timer timer = PacketAdapter.toTimer(p);
                        onTimerAdded(timer);
                    }
                    postOnTimersUpdate();
                    break;
            }
        }

        @Override
        public void onDisconnect() {
            DataService.this.connectionStatus = STATUS_Server_NotConnected;
            postOpen();
        }
    };

    private final Runnable mOpenRunnable = new Runnable() {
        @Override
        public void run() {
            if(!open()) {
                if(!TextUtils.isEmpty(SetupUtils.getServer(DataService.this))) {
                    if(wasConnected) {
                        notification.error(getResources().getString(R.string.failed_connect));
                    }
                    wasConnected = false;
                }
                handler.postDelayed(mOpenRunnable, 10 * 1000);
                return;
            }

            if(!wasConnected) {
                notification.notify(getResources().getString(R.string.service_connected));
                wasConnected = true;
            }
        }
    };

    private final ArraySet<Listener> listeners = new ArraySet<>();

    public DataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service id: " + startId);

        if(startId != 1) {
            return START_STICKY;
        }

        // check if the server has changed
        String server = SetupUtils.getServer(this);

        if(connectionStatus == STATUS_Server_NotConnected) {
            postOpen();
        }
        else if(!connection.getHostname().equals(server)) {
            Log.i(TAG, "new server: " + server);
            connection.close();
            connectionStatus = STATUS_Server_NotConnected;

            postOpen();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        handlerThread = new HandlerThread("dataservice");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        listenerHandler = new Handler(Looper.getMainLooper());

        notification = new NotificationHandler(this);

        connection = new Connection(
            "roboTV:dataservice",
            SetupUtils.getLanguage(this),
            true);

        connection.setCallback(mSessionListener);

        String language = SetupUtils.getLanguage(this);
        artwork = new ArtworkFetcher(connection, language);

        movieController = new MovieController(connection);
        timerController = new TimerController(connection);
        channel = new RoboTVChannel(this);
    }

    public void updateRecommendationChannel() {
        channel.create();
        channel.update();
    }

    @Override
    public void onDestroy() {
        connectionStatus = STATUS_Server_NotConnected;
        connection.close();
        handlerThread.interrupt();
    }

    private boolean open() {
        Log.d(TAG, "open");
        if(connectionStatus == STATUS_Server_Connected) {
            postOnConnected();
            return true;
        }

        String server = SetupUtils.getServer(this);

        if(TextUtils.isEmpty(server)) {
            connectionStatus = STATUS_Server_NotConnected;
            postOnConnectionError();
            return false;
        }

        connectionStatus = STATUS_Server_Connecting;
        connection.setPriority(1); // low priority for DataService

        Log.i(TAG, "connecting to " + server + "...");

        if(!connection.open(server)) {
            connection.close();
            connectionStatus = STATUS_Server_NotConnected;
            postOnConnectionError();

            return false;
        }

        updateRecommendationChannel();

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

        final Event event = PacketAdapter.toEvent(p);

        handler.post(() -> {
            try {
                if(artwork.fetchForEvent(event)) {
                    notification.notify(message, event.getTitle(), event.getBackgroundUrl());
                }
                else {
                    notification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                notification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
            }
        });

        // TODO - move to movie update handler
        channel.update();
    }

    public String getSeriesFolder() {
        if(seriesFolder != null) {
            return seriesFolder;
        }

        seriesFolder = connection.getConfig("SeriesFolder");

        if(TextUtils.isEmpty(seriesFolder)) {
            seriesFolder = "Shows";
        }

        return seriesFolder;
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

    public TimerController getTimerController() {
        return timerController;
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

        listenerHandler.post(() -> listener.onConnected(DataService.this));
    }

    private void postOnConnected() {
        Log.d(TAG, "postOnConnected");
        listenerHandler.post(() -> {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.valueAt(i).onConnected(DataService.this);
            }
            connectionStatus = STATUS_Server_Connected;
        });
    }

    private void postOnConnectionError() {
        Log.d(TAG, "postOnConnectionError");
        listenerHandler.post(() -> {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.valueAt(i).onConnectionError(DataService.this);
            }
        });
    }

    private void postOnMovieUpdate() {
        Log.d(TAG, "postOnMovieUpdate");
        channel.update();

        listenerHandler.post(() -> {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.valueAt(i).onMovieUpdate(DataService.this);
            }
        });
    }

    private void postOnTimersUpdate() {
        listenerHandler.post(() -> {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.valueAt(i).onTimersUpdated(DataService.this);
            }
        });
    }

    private void onTimerAdded(final Timer timer) {
        handler.post(() -> {
            String url = timer.getLogoUrl();


            try {
                if(artwork.fetchForEvent(timer)) {
                    url = timer.getPosterUrl();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            notification.notify(getString(R.string.timer_created), timer.getTitle(), url);
        });
    }

    static public void cancelSyncJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
    }

    static public void scheduleSyncJob(Context context, boolean force) {
        // schedule sync job
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        String inputId = SetupUtils.getInputId(context);

        if(inputId == null) {
            Log.e(TAG, "unable to schedule sync job (inputId == null)");
            return;
        }

        if(jobScheduler == null) {
            Log.e(TAG, "unable to schedule sync job (jobScheduler == null)");
            return;
        }

        if(jobScheduler.getPendingJob(SYNC_JOB_ID) != null && !force) {
            Log.d(TAG, "sync job already pending");
            return;
        }

        JobInfo jobInfo = new JobInfo.Builder(SYNC_JOB_ID,
                new ComponentName(context, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(1000 * 60 * 60) // 1 hour
                .setPersisted(true)
                .build();

        Log.d(TAG, "added sync job to scheduler");
        jobScheduler.schedule(jobInfo);

    }
}
