package org.xvdr.robotv.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.SessionListener;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.ArtworkUtils;
import org.xvdr.robotv.artwork.Event;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.IOException;

public class DataService extends Service {

    Connection mConnection;
    Handler mHandler;
    NotificationHandler mNotification;
    ArtworkFetcher m_artwork;

    private static final String TAG = "DataService";

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
                    mNotification.notify(message);
                    break;

                case Connection.XVDR_STATUS_RECORDING:
                    onRecording(p);
                    break;
            }
        }

        public void onDisconnect() {
        }

        public void onReconnect() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mConnection.login();
                }
            }, 3000);
        }
    };

    private Runnable mOpenRunnable = new Runnable() {
        @Override
        public void run() {
            if(!open()) {
                mHandler.postDelayed(mOpenRunnable, 2 * 1000);
                return;
            }

            mNotification.notify(getResources().getString(R.string.service_connected));
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start service");

        // check if the server has changed
        String server = SetupUtils.getServer(this);

        if(mConnection != null && !mConnection.getHostname().equals(server)) {
            Log.i(TAG, "new server: " + server);
            mConnection.close();
            mHandler.removeCallbacks(mOpenRunnable);
            mHandler.post(mOpenRunnable);
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mNotification = new NotificationHandler(this);

        mConnection = new Connection(
            "roboTV:dataservice",
            SetupUtils.getLanguage(this),
            true);

        mConnection.setCallback(mSessionListener);

        m_artwork = new ArtworkFetcher(mConnection, SetupUtils.getLanguage(this));

        mHandler.post(mOpenRunnable);
    }

    @Override
    public void onDestroy() {
        mConnection.close();
    }

    private boolean open() {
        if(mConnection.isOpen()) {
            return true;
        }

        mConnection.setPriority(1); // low priority for DataService
        return mConnection.open(SetupUtils.getServer(this));
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
            mNotification.notify(message, title, R.drawable.ic_movie_white_48dp);
            return;
        }

        // process attached event

        final Event event = ArtworkUtils.packetToEvent(p);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ArtworkHolder holder = m_artwork.fetchForEvent(event);

                    if(holder != null) {
                        mNotification.notify(message, event.getTitle(), holder.getBackgroundUrl());
                    }
                    else {
                        mNotification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                    mNotification.notify(message, event.getTitle(), R.drawable.ic_movie_white_48dp);
                }
            }
        });
    }

}
