package org.xvdr.robotv.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import org.xvdr.recordings.model.Movie;

import java.util.Collection;

public class DataServiceClient {

    public interface Listener {
        void onServiceConnected(DataService service);
        void onServiceDisconnected(DataService service);
        void onMovieCollectionUpdated(DataService service, Collection<Movie> collection, int status);
    }

    private DataService service;
    private Context context;
    private Listener listener;
    private Object lock = new Object();

    Handler handler;

    private DataService.Listener dataServiceListener = new DataService.Listener() {
        @Override
        public void onMovieCollectionUpdated(final Collection<Movie> collection, final int status) {
            if(listener == null) {
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onMovieCollectionUpdated(service, collection, status);
                }
            });
        }
    };

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            DataService.Binder binder = (DataService.Binder) serviceBinder;
            service = binder.getService();

            service.registerListener(dataServiceListener);

            if(listener == null) {
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onServiceConnected(service);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service.unregisterListener(dataServiceListener);

            if(listener != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onServiceDisconnected(service);
                    }
                });
            }

            service = null;

        }
    };

    public DataServiceClient(Context context) {
        this(context, null);
    }

    public DataServiceClient(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.handler = new Handler();
    }

    public boolean bind() {
        Intent serviceIntent = new Intent(context, DataService.class);
        context.startService(serviceIntent);

        return context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        context.unbindService(connection);
    }

    public DataService getService() {
        return service;
    }
}
