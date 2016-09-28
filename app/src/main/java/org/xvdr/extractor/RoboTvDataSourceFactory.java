package org.xvdr.extractor;

import android.os.Handler;

import com.google.android.exoplayer2.upstream.DataSource;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.SessionListener;
import org.xvdr.robotv.client.Connection;

import java.io.IOError;
import java.io.IOException;

public class RoboTvDataSourceFactory implements DataSource.Factory {

    private Connection connection;

    final private Listener listener;
    final private RoboTvDataSource dataSource;

    interface Listener {

        void onDisconnect();

        void onReconnect();

    }

    private SessionListener sessionListener = new SessionListener() {

        private Handler handler = new Handler();

        public void onNotification(Packet p) {
        }

        public void onDisconnect() {
            if(connection == null || listener == null) {
                return;
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listener.onDisconnect();
                }
            }, 3000);
        }

        public void onReconnect() {
            if(connection == null || listener == null) {
                return;
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connection.login();
                    listener.onReconnect();
                }
            }, 3000);
        }
    };

    RoboTvDataSourceFactory(PositionReference position, String language, Listener listener) {
        connection = new Connection("roboTV:streaming", language);
        connection.setCallback(sessionListener);

        dataSource = new RoboTvDataSource(position, connection, language);

        this.listener = listener;
    }

    @Override
    public DataSource createDataSource() {
        return dataSource;
    }

    public boolean connect(String server) throws IOException {
        if(connection.isOpen()) {
            return false;
        }

        if(connection.open(server)) {
            return true;
        }

        throw new IOException("unable to connect to server");
    }

    public void release() {
        if(dataSource != null) {
            dataSource.release();
        }

        connection.close();
    }

}
