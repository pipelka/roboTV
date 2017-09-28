package org.xvdr.recordings.presenter;

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

import com.bumptech.glide.Glide;

import org.xvdr.robotv.artwork.ArtworkFetcher;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.setup.SetupUtils;

import java.io.IOException;

public class MoviePresenter extends Presenter {

    protected Connection connection = null;

    public MoviePresenter(@NonNull Connection connection) {
        this.connection = connection;
    }

    static public class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;

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

            Glide.with(mCardView.getContext())
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

            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
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
                    updateCardViewImage(context, url);
                }
            };

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
