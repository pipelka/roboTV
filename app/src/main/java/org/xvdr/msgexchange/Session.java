package org.xvdr.msgexchange;

import java.util.concurrent.CopyOnWriteArraySet;

public class Session extends SessionProxy {
    private final CopyOnWriteArraySet<Callback> mCallbacks;

    public interface Callback {
        void onNotification(Packet notification);
        void onDisconnect();
        void onReconnect();
    }

    public Session() {
        mCallbacks = new CopyOnWriteArraySet<>();
    }

    public void addCallback(Callback callback) {
        if(callback == null) {
            return;
        }

        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        if(callback == null) {
            return;
        }

        mCallbacks.remove(callback);
    }

    public void removeAllCallbacks() {
        mCallbacks.clear();
    }

    @Override
    protected void onNotification(final Packet notification) {
        for(Callback cb : mCallbacks) {
            cb.onNotification(notification);
        }
    }

    @Override
    protected void onDisconnect() {
        for(Callback cb : mCallbacks) {
            cb.onDisconnect();
        }
    }

    @Override
    protected void onReconnect() {
        for(Callback cb : mCallbacks) {
            cb.onReconnect();
        }
    }
}
