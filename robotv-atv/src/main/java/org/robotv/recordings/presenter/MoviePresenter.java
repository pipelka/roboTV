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

import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.Connection;
import org.robotv.client.model.Movie;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.GlideApp;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoviePresenter extends Presenter {

    static private final ExecutorService threadPool = Executors.newCachedThreadPool();

    protected final Connection connection;

    public MoviePresenter(@NonNull Connection connection) {
        this.connection = connection;
    }

    public static class ViewHolder extends Presenter.ViewHolder {
        private final ImageCardView mCardView;
        private final Drawable mDrawableUnknown;

        private void updateTask(ArtworkFetcher artwork, Movie movie, ViewHolder view) {
            String url = null;

            try {
                artwork.fetchForEvent(movie);
                url = movie.getPosterUrl();
                if(TextUtils.isEmpty(url)) {
                    url = movie.getBackgroundUrl();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            final String finalUrl = url;
            view.mCardView.post(() -> updateCardViewImage(finalUrl));
        }

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
            mDrawableUnknown = view.getContext().getDrawable(R.drawable.recording_unkown);
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        private void updateCardViewImage(String link) {
            if(link == null || link.isEmpty() || link.equals("x")) {
                mCardView.setMainImage(mDrawableUnknown);
                return;
            }


            GlideApp.with(mCardView)
                .load(link)
                .override(266, 400)
                .centerCrop()
                .error(mDrawableUnknown)
                .placeholder(mDrawableUnknown)
                .into(mCardView.getMainImageView());
        }

        public void update(final Movie movie, Connection connection, final Context context) {
            String language = SetupUtils.getLanguage(context);
            final ArtworkFetcher artwork = new ArtworkFetcher(connection, language);

            threadPool.execute(() -> updateTask(artwork, movie, ViewHolder.this));
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
        Movie movie = (Movie) item;
        ViewHolder vh = (ViewHolder) viewHolder;
        ImageCardView cardView = vh.getCardView();
        Context context = cardView.getContext();
        Resources resources = cardView.getResources();
        cardView.setTitleText(movie.getTitle());

        if(movie.isSeriesHeader()) {
            int count = movie.getEpisodeCount();
            cardView.setContentText(
                    count + " " + resources.getString(count > 1 ? R.string.episodes : R.string.episode));
        }
        else {
            cardView.setContentText(movie.getShortText());
        }

        cardView.setInfoAreaBackgroundColor(Utils.getColor(cardView.getContext(), R.color.primary_color));
        cardView.setMainImageDimensions(266, 400);

        vh.update(movie, connection, context);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
