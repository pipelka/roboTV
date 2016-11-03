package org.xvdr.recordings.util;

import android.graphics.Bitmap;
import android.support.v17.leanback.app.BackgroundManager;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

public class BackgroundManagerTarget extends SimpleTarget<Bitmap> {
    private BackgroundManager backgroundManager;

    public BackgroundManagerTarget(BackgroundManager backgroundManager) {
        this.backgroundManager = backgroundManager;
    }

    @Override
    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
        backgroundManager.setBitmap(resource);
    }
}
