package org.robotv.recordings.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
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

public class MoviePresenter extends Presenter {

    protected Connection connection;

    public MoviePresenter(@NonNull Connection connection) {
        this.connection = connection;
    }

    static public class ViewHolder extends Presenter.ViewHolder {
        private final ImageCardView mCardView;

        static class UpdateTask extends AsyncTask<Void, Void, String> {
            final ArtworkFetcher artwork;
            final Movie movie;
            final ViewHolder view;

            public UpdateTask(ArtworkFetcher artwork, Movie movie, ViewHolder view) {
                this.artwork = artwork;
                this.movie = movie;
                this.view = view;
            }

            @Override
            protected String doInBackground(Void... params) {
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
                return url;
            }

            protected void onPostExecute(String url) {
                view.updateCardViewImage(view.getCardView().getContext(), url);
            }
        }

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        private void updateCardViewImage(Context context, String link) {
            Drawable drawableUnknown = context.getDrawable(R.drawable.recording_unkown);

            if(link == null || link.isEmpty() || link.equals("x")) {
                mCardView.setMainImage(drawableUnknown);
                return;
            }

            GlideApp.with(context)
            .load(link)
            .override(266, 400)
            .centerCrop()
            .error(drawableUnknown)
            .placeholder(drawableUnknown)
            .into(mCardView.getMainImageView());
        }

        public void update(final Movie movie, Connection connection, final Context context) {
            String language = SetupUtils.getLanguage(context);
            final ArtworkFetcher artwork = new ArtworkFetcher(connection, language);

            UpdateTask task = new UpdateTask(artwork, movie, this);
            task.execute();
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
