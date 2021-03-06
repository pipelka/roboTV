package org.robotv.recordings.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.load.DecodeFormat;

import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.Connection;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.model.Event;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.MovieImageCardView;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.setup.SetupUtils;
import org.robotv.ui.ExecutorPresenter;
import org.robotv.ui.GlideApp;

import java.io.IOException;
public class MoviePresenter extends ExecutorPresenter {

    private static final String TAG = MoviePresenter.class.getName();

    protected final Connection connection;
    protected final boolean changeBackground;

    final static public int WIDTH = 266;
    final static public int HEIGHT = 400;
    protected OnLongClickListener listener;

    public interface OnLongClickListener {
        boolean onLongClick(Movie movie);
    }

    public MoviePresenter(@NonNull Connection connection, boolean changeBackground) {
        this.connection = connection;
        this.changeBackground = changeBackground;
    }

    public static class ViewHolder extends Presenter.ViewHolder {
        private final MovieImageCardView mCardView;
        private final Drawable mDrawableUnknown;

        private void setArtwork(ArtworkHolder artwork) {
            String url = artwork.getPosterUrl();

            if(TextUtils.isEmpty(url)) {
                url = artwork.getBackgroundUrl();
            }

            final String finalUrl = url;
            mCardView.setBackgroundUrl(artwork.getBackgroundUrl());
            mCardView.post(() -> updateCardViewImage(finalUrl));
        }

        private void updateTask(ArtworkFetcher artwork, Movie movie) {
            try {
                artwork.fetchForEvent(movie);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            setArtwork(movie);
        }

        public ViewHolder(View view) {
            super(view);
            mCardView = (MovieImageCardView) view;
            mDrawableUnknown = view.getContext().getDrawable(R.drawable.recording_unkown);
        }

        public MovieImageCardView getCardView() {
            return mCardView;
        }

        private void updateCardViewImage(String link) {
            if(link == null || link.isEmpty() || link.equals("x")) {
                mCardView.setMainImage(mDrawableUnknown);
                return;
            }


            GlideApp.with(mCardView)
                .asBitmap()
                .format(DecodeFormat.PREFER_RGB_565)
                .load(link)
                .override(WIDTH, HEIGHT)
                .centerCrop()
                .error(mDrawableUnknown)
                .placeholder(mDrawableUnknown)
                .into(mCardView.getMainImageView());
        }

        public void update(final Movie movie, Connection connection, final Context context) {
            String language = SetupUtils.getLanguage(context);
            final ArtworkFetcher artwork = new ArtworkFetcher(connection, language);

            if(movie.hasArtwork()) {
                setArtwork(movie);
            }
            else {
                execute(() -> updateTask(artwork, movie));
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        MovieImageCardView cardView = new MovieImageCardView(parent.getContext(), changeBackground);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setLongClickable(true);

        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        ViewHolder vh = (ViewHolder) viewHolder;

        MovieImageCardView cardView = vh.getCardView();
        cardView.setTitleText(movie.getTitle());

        Context context = cardView.getContext();
        Resources resources = cardView.getResources();

        if(movie.isSeriesHeader()) {
            int count = movie.getEpisodeCount();
            cardView.setContentText(
                    count + " " + resources.getString(count > 1 ? R.string.episodes : R.string.episode));
        }
        else if(movie.isTvShow()) {
            Event.SeasonEpisodeHolder holder = movie.getSeasionEpisode();
            cardView.setTitleText(movie.getShortText());
            cardView.setContentText(resources.getString(R.string.season_episode, holder.season, holder.episode));
        }
        else {
            cardView.setContentText(movie.getShortText());
        }

        cardView.setInfoAreaBackgroundColor(Utils.getColor(cardView.getContext(), R.color.primary_color));
        cardView.setMainImageDimensions(MoviePresenter.WIDTH, MoviePresenter.HEIGHT);
        cardView.setOnLongClickListener(v -> this.onLongClick(movie));

        vh.update(movie, connection, context);
    }

    private boolean onLongClick(Movie movie) {
        if(listener == null) {
            return false;
        }

        return listener.onLongClick(movie);
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        this.listener = listener;

    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.getCardView().setOnLongClickListener(null);
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }
}
