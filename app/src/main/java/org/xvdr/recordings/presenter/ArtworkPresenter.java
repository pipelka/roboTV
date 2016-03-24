package org.xvdr.recordings.presenter;

import android.content.Context;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import org.xvdr.recordings.util.PicassoImageCardViewTarget;
import org.xvdr.recordings.util.Utils;
import org.xvdr.robotv.R;
import org.xvdr.robotv.artwork.ArtworkHolder;


public class ArtworkPresenter extends Presenter {

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
            .error(context.getDrawable(R.drawable.recording_unkown))
            .placeholder(context.getDrawable(R.drawable.recording_unkown))
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
        ArtworkHolder holder = (ArtworkHolder) item;
        ViewHolder vh = (ViewHolder) viewHolder;

        ImageCardView cardView = vh.getCardView();
        Context context = cardView.getContext();

        cardView.setTitleText(holder.getTitle());
        cardView.setMainImageDimensions(266, 400);
        cardView.setInfoAreaBackgroundColor(Utils.getColor(context, R.color.primary_color));

        vh.updateCardViewImage(cardView.getContext(), holder.getPosterUrl());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
