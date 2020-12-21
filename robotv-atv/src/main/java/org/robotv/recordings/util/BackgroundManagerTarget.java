package org.robotv.recordings.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import jp.wasabeef.glide.transformations.gpu.SepiaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.VignetteFilterTransformation;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.robotv.ui.GlideApp;

public class BackgroundManagerTarget extends CustomTarget<Drawable> {
    private final BackgroundManager backgroundManager;

    public static final MultiTransformation<Bitmap> transformation = new MultiTransformation<>(
            new SepiaFilterTransformation(),
            new VignetteFilterTransformation()
    );

    public BackgroundManagerTarget(BackgroundManager backgroundManager) {
        this.backgroundManager = backgroundManager;
    }

    @Override
    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
        try {
            backgroundManager.setDrawable(resource);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
    }

    static public void setBackground(String url, Activity activity) {
        setBackground(url, activity, null);
    }

    static public void setBackground(String url, Activity activity, Runnable runnable) {
        if(activity.isDestroyed()) {
            return;
        }

        BackgroundManager manager = BackgroundManager.getInstance(activity);
        if(!manager.isAttached()) {
            manager.attach(activity.getWindow());
        }

        CustomTarget<Drawable> target = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                manager.setDrawable(resource);
                if(runnable != null) {
                    activity.runOnUiThread(runnable);
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };

        if(TextUtils.isEmpty(url) || url.equals("x")) {
            manager.clearDrawable();
        }
        else {
            GlideApp.with(activity)
                    .load(url)
                    .apply(RequestOptions.bitmapTransform(transformation))
                    //.format(DecodeFormat.PREFER_RGB_565)
                    .into(target);
        }
    }
}
