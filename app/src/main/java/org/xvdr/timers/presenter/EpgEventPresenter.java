package org.xvdr.timers.presenter;

import android.content.Context;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

public class EpgEventPresenter extends Presenter {

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
            if(link == null || link.isEmpty()) {
                mCardView.setMainImage(context.getDrawable(R.drawable.recording_unkown));
                return;
            }

            Glide.with(context)
            .load(link)
            .override(Utils.dp(R.integer.artwork_background_width, context), Utils.dp(R.integer.artwork_background_height, context))
            .centerCrop()
            .error(context.getDrawable(R.drawable.recording_unkown))
            .placeholder(context.getDrawable(R.drawable.recording_unkown))
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
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }


    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        ViewHolder vh = (ViewHolder) viewHolder;
        ImageCardView cardView = vh.getCardView();
        Context context = cardView.getContext();

        cardView.setTitleText(movie.getTitle());
        String subText = (movie.getOutline().isEmpty() ? "" : movie.getOutline() + " - ");
        subText += movie.getDateTime();

        cardView.setContentText(subText);

        cardView.setMainImageDimensions(
            Utils.dp(R.integer.artwork_background_width, context) * 2 / 3,
            Utils.dp(R.integer.artwork_background_height, context) * 2 / 3);

        cardView.setInfoAreaBackgroundColor(Utils.getColor(context, R.color.primary_color));
        vh.updateCardViewImage(context, movie.getBackgroundImageUrl());
    }

}
