package org.xvdr.robotv.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DataServiceClient {

    private DataService service;
    private Context context;
    private DataService.Listener listener;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            DataService.Binder binder = (DataService.Binder) serviceBinder;
            service = binder.getService();

            service.registerListener(listener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    public DataServiceClient(Context context) {
        this(context, null);
    }

    public DataServiceClient(Context context, DataService.Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public boolean bind() {
        Intent serviceIntent = new Intent(context, DataService.class);
        context.startService(serviceIntent);
        return context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        if(service != null) {
            service.unregisterListener(listener);
        }

        context.unbindService(connection);
    }

    public DataService getService() {
        return service;
    }
}
