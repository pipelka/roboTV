package org.xvdr.robotv.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

public class DataService extends Service implements Connection.Callback {

    Connection mConnection;
    Handler mHandler;
    NotificationHandler mNotification;

    private static final String TAG = "DataService";

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
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        mHandler = new Handler();
        mNotification = new NotificationHandler(this);

        mConnection = new Connection(
            "roboTV:dataservice",
            SetupUtils.getLanguage(this),
            true);

        mConnection.addCallback(this);
        mHandler.post(mOpenRunnable);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mConnection.close();
    }

    private boolean open() {
        return mConnection.open(SetupUtils.getServer(this));
    }

    @Override
    public void onNotification(Packet notification) {
        String message;

        // process only STATUS messages
        if(notification.getType() != Connection.XVDR_CHANNEL_STATUS) {
            return;
        }

        int id = notification.getMsgID();
        Log.d(TAG, "notification id: " + id);

        switch(id) {
            case Connection.XVDR_STATUS_MESSAGE:
                Log.d(TAG, "status message");
                notification.getU32(); // type
                message = notification.getString();
                mNotification.notify(message);
                break;

            case Connection.XVDR_STATUS_RECORDING:
                Log.d(TAG, "recording status");
                notification.getU32(); // card index
                int on = (int) notification.getU32(); // on

                String recname = notification.getString(); // name
                notification.getString(); // filename

                message = getResources().getString(R.string.recording_text) + " ";
                message += (on == 1) ?
                           getResources().getString(R.string.recording_started) :
                           getResources().getString(R.string.recording_finished);

                mNotification.notify(recname, message, R.drawable.ic_movie_white_48dp);
                break;
        }
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onReconnect() {
    }

}
