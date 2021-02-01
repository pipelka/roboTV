package org.robotv.dataservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DataServiceClient {

    private DataService service;
    private final Context context;
    private final DataService.Listener listener;

    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            DataService.Binder binder = (DataService.Binder) serviceBinder;
            service = binder.getService();

            if(listener != null) {
                service.registerListener(listener);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    public DataServiceClient(Context context, DataService.Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public DataServiceClient(Context context) {
        this.context = context;
        this.listener = null;
    }

    public void bind() {
        Intent serviceIntent = new Intent(context, DataService.class);
        context.startService(serviceIntent);
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        if(service != null && listener != null) {
            service.unregisterListener(listener);
        }

        context.unbindService(connection);
    }

    public void reconnect() {
        Intent serviceIntent = new Intent(context, DataService.class);
        context.stopService(serviceIntent);
        context.startService(serviceIntent);
    }

    public DataService getService() {
        return service;
    }
}
