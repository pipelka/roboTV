package org.xvdr.recordings.presenter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import org.xvdr.recordings.model.Movie;
import org.xvdr.recordings.util.PicassoImageCardViewTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;


public class MoviePresenter extends Presenter {

    static Picasso picasso = null;

    static public class ViewHolder extends Presenter.ViewHolder {
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
            Drawable drawableUnknown = context.getDrawable(R.drawable.recording_unkown);

            if(link == null || link.isEmpty()) {
                mCardView.setMainImage(drawableUnknown);
                return;
            }

            picasso
            .load(link)
            .resize(266, 400)
            .centerCrop()
            .placeholder(drawableUnknown)
            .error(drawableUnknown)
            .placeholder(drawableUnknown)
            .into(mImageCardViewTarget);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        if(picasso == null) {
            picasso = new Picasso.Builder(parent.getContext())
                    .memoryCache(new LruCache(10 * 1024 * 1024))
                    .build();
        }

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
