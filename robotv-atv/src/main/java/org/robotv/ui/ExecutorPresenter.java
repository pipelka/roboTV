package org.robotv.ui;

import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.leanback.widget.Presenter;

public class ExecutorPresenter extends Presenter {

    static private final ExecutorService threadPool = Executors.newCachedThreadPool();

    static protected void execute(Runnable runnable) {
        threadPool.execute(runnable);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
