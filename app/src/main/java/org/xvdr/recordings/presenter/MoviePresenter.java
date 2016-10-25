package org.xvdr.recordings.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

public class MoviePresenter extends Presenter {

    static public class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(Context context, String link) {
            Drawable drawableUnknown = context.getDrawable(R.drawable.recording_unkown);

            if(link == null || link.isEmpty()) {
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
        Resources resources = cardView.getResources();
        cardView.setTitleText(movie.getTitle());

        if(movie.isSeriesHeader()) {
            int count = movie.getEpisodeCount();
            cardView.setContentText(
                    count + " " + resources.getString(count > 1 ? R.string.episodes : R.string.episode));
        }
        else {
            cardView.setContentText(movie.getOutline());
        }

        cardView.setInfoAreaBackgroundColor(Utils.getColor(cardView.getContext(), R.color.primary_color));
        cardView.setMainImageDimensions(266, 400);

        vh.updateCardViewImage(cardView.getContext(), movie.getCardImageUrl());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
