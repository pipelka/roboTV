package org.robotv.recordings.model;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.robotv.recordings.util.BackgroundManagerTarget;
import org.robotv.ui.GlideApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.widget.ImageCardView;
import jp.wasabeef.glide.transformations.gpu.*;

@SuppressLint("ViewConstructor")
public class MovieImageCardView extends ImageCardView {

    private static final String TAG = MovieImageCardView.class.getName();

    private String backgroundUrl;
    protected final boolean changeBackground;
    private final BackgroundManager manager;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    public MovieImageCardView(Context context, boolean changeBackground) {
        super(context);
        setLongClickable(true);

        final Activity activity = getActivity(context);

        this.changeBackground = changeBackground;
        this.manager = BackgroundManager.getInstance(activity);
    }

    private Activity getActivity(Context context) {
        if (context == null) {
            return null;
        }
        else if (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            else {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        if(!changeBackground) {
            return;
        }

        final Activity activity = getActivity(getContext());

        if(activity == null || activity.isDestroyed()) {
            return;
        }

        if(manager == null) {
            return;
        }

        if(!manager.isAttached()) {
            manager.attach(activity.getWindow());
        }

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> setBackground(backgroundUrl, selected, null), 500);
    }

    @Override
    public boolean performClick() {
        if(!changeBackground) {
            return super.performClick();
        }

        setBackground(backgroundUrl, true, super::performClick);
        return true;
    }

    private void setBackground(String url, boolean selected, Runnable callback) {
        BackgroundManagerTarget.setBackground(url, getActivity(getContext()), callback);
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }
}
