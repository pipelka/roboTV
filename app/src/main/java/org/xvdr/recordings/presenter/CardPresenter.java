package org.xvdr.recordings.presenter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.PicassoImageCardViewTarget;
import org.xvdr.robotv.R;

import com.squareup.picasso.Picasso;


public class CardPresenter extends Presenter {

    static class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;
        private PicassoImageCardViewTarget mImageCardViewTarget;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
            mImageCardViewTarget = new PicassoImageCardViewTarget(mCardView);
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(Context context, String link) {
            if(link == null || link.isEmpty()) {
                mCardView.setMainImage(context.getDrawable(R.drawable.recording_unkown));
                return;
            }

            Picasso.with(context)
            .load(link)
            .resize(266, 400)
            .centerCrop()
            .error(new ColorDrawable(0))
            .into(mImageCardViewTarget);
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

        cardView.setTitleText(movie.getTitle());
        cardView.setContentText(movie.getOutline());
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
