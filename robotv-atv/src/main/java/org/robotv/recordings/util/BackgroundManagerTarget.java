package org.robotv.recordings.util;

import android.graphics.drawable.Drawable;
import android.support.v17.leanback.app.BackgroundManager;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

public class BackgroundManagerTarget extends SimpleTarget<Drawable> {
    private BackgroundManager backgroundManager;

    public BackgroundManagerTarget(BackgroundManager backgroundManager) {
        this.backgroundManager = backgroundManager;
    }

    @Override
    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
        backgroundManager.setDrawable(resource);
    }
}
