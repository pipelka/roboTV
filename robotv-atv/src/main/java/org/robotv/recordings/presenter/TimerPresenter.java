package org.robotv.recordings.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.robotv.recordings.model.EpisodeTimer;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.Connection;
import org.robotv.client.model.Timer;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.ExecutorPresenter;
import org.robotv.ui.GlideApp;

import java.io.IOException;

public class TimerPresenter extends ExecutorPresenter {

    private final Connection connection;

    private void timerArtworkTask(ArtworkFetcher artwork, Timer timer, ImageCardView cardView) {
        Drawable drawableUnknown = cardView.getResources().getDrawable(R.drawable.recording_unkown, null);

        try {
            artwork.fetchForEvent(timer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        cardView.post(() -> {
            if(!TextUtils.isEmpty(timer.getLogoUrl())) {
                GlideApp.with(cardView.getContext())
                        .load(timer.getLogoUrl())
                        .placeholder(R.drawable.recording_unkown)
                        .into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                cardView.setBadgeImage(resource);
                            }
                        });
            }

            String imageUrl = TextUtils.isEmpty(timer.getBackgroundUrl()) ? timer.getLogoUrl() : timer.getBackgroundUrl();

            if(TextUtils.isEmpty(imageUrl) || imageUrl.equals("x")) {
                cardView.setMainImage(drawableUnknown);
                return;
            }

            cardView.setMainImage(drawableUnknown);

            GlideApp.with(cardView.getContext())
                    .load(imageUrl)
                    .error(drawableUnknown)
                    .placeholder(drawableUnknown)
                    .fallback(drawableUnknown)
                    .override(391, 220)
                    .centerCrop()
                    .into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                            cardView.setMainImage(resource);
                        }
                    });
        });
    }

    public TimerPresenter(@NonNull Connection connection) {
        this.connection = connection;
    }

    static public class ViewHolder extends Presenter.ViewHolder {
        private final ImageCardView cardView;

        public ViewHolder(View view) {
            super(view);
            cardView = (ImageCardView) view;
        }

        ImageCardView getCardView() {
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
        final Timer timer = (Timer) item;
        final ViewHolder vh = (ViewHolder) viewHolder;
        final ImageCardView cardView = vh.getCardView();
        final Context context = cardView.getContext();
        Resources resources = cardView.getResources();

        cardView.setTitleText(timer.getTitle());

        String date = timer.getDate();
        String time = timer.getTime();

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
        cardView.getMainImageView().setPadding(0, 0, 0, 0);

        String language = SetupUtils.getLanguage(context);
        ArtworkFetcher artwork = new ArtworkFetcher(connection, language);

        execute(() -> timerArtworkTask(artwork, timer, cardView));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
