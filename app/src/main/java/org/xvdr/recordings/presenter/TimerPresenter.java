package org.xvdr.recordings.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.xvdr.recordings.model.EpisodeTimer;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.model.Timer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimerPresenter extends Presenter {

    static public class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView cardView;

        public ViewHolder(View view) {
            super(view);
            cardView = (ImageCardView) view;
        }

        public ImageCardView getCardView() {
            return cardView;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext());
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Timer timer = (Timer) item;
        ViewHolder vh = (ViewHolder) viewHolder;
        final ImageCardView cardView = vh.getCardView();
        Resources resources = cardView.getResources();

        cardView.setTitleText(timer.getTitle());

        String date = timer.getDate();
        String time = new SimpleDateFormat("HH:mm").format(new Date((timer.getStartTime() * 1000)));

        if(item instanceof EpisodeTimer) {
            EpisodeTimer episodeTimer = (EpisodeTimer) item;
            int count = episodeTimer.getTimerCount();

            cardView.setContentText(
                date + " " + " / " +
                count + " " +
                (count > 1 ? resources.getString(R.string.episodes) : resources.getString(R.string.episode)));
        }
        else {
            cardView.setContentText(date + " " + time + " / " + (timer.getDuration() / 60) + " min");
        }

        cardView.setInfoAreaBackgroundColor(
                Utils.getColor(cardView.getContext(),
                timer.isRecording() ? R.color.recording_active_color : R.color.primary_color));

        cardView.setMainImageDimensions(391, 220);
        Drawable drawableUnknown = resources.getDrawable(R.drawable.recording_unkown, null);

        if(!TextUtils.isEmpty(timer.getLogoUrl())) {
            Glide.with(cardView.getContext())
                    .load(timer.getLogoUrl())
                    .error(drawableUnknown)
                    .placeholder(drawableUnknown)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            cardView.setBadgeImage(resource);
                        }
                    });
        }

        Glide.with(cardView.getContext())
                .load(TextUtils.isEmpty(timer.getPosterUrl()) ? timer.getLogoUrl() : timer.getPosterUrl())
                .centerCrop()
                .error(drawableUnknown)
                .placeholder(drawableUnknown)
                .into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        cardView.setMainImage(resource);
                    }
                });
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
