package org.xvdr.player.source;

import android.os.Handler;

import com.google.android.exoplayer2.upstream.DataSource;

import org.xvdr.jniwrap.Packet;
import org.xvdr.jniwrap.SessionListener;
import org.xvdr.player.PositionReference;
import org.xvdr.robotv.client.Connection;

import java.io.IOException;

public class RoboTvDataSourceFactory implements DataSource.Factory {

    private Connection connection;

    final private Listener listener;
    final private RoboTvDataSource dataSource;

    public interface Listener {

        void onDisconnect();

        void onReconnect();

        void onStreamError(int status);

        void onServerTuned(int status);
    }

    private SessionListener sessionListener = new SessionListener() {

        private Handler handler = new Handler();

        public void onNotification(Packet p) {
        }

        public void onDisconnect() {
            if(connection == null || listener == null) {
                return;
            }

            try {
                dataSource.disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
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
                    // reschedule login if login fails
                    if(!connection.login()) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sessionListener.onReconnect();
                            }
                        }, 1000);
                        return;
                    }

                    // notify about successful reconnect
                    listener.onReconnect();
                }
            }, 3000);
        }
    };

    public RoboTvDataSourceFactory(PositionReference position, String language, Listener listener) {
        this.listener = listener;

        connection = new Connection("roboTV:streaming", language);
        connection.setCallback(sessionListener);

        dataSource = new RoboTvDataSource(position, connection, language, new RoboTvDataSource.Listener() {
            @Override
            public void onOpenStreamError(int status) {
                if(RoboTvDataSourceFactory.this.listener != null) {
                    RoboTvDataSourceFactory.this.listener.onStreamError(status);
                }
            }

            @Override
            public void onServerTuned(int status) {
                if(RoboTvDataSourceFactory.this.listener != null) {
                    RoboTvDataSourceFactory.this.listener.onServerTuned(status);
                }
            }
        });

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

    public Connection getConnection() {
        return connection;
    }
}
