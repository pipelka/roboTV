package org.robotv.recordings.presenter;

import android.content.Context;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import org.robotv.recordings.model.MovieImageCardView;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.ui.GlideApp;


public class ArtworkPresenter extends Presenter {

    static class ViewHolder extends Presenter.ViewHolder {
        private final MovieImageCardView mCardView;

        public ViewHolder(View view) {
            super(view);
            mCardView = (MovieImageCardView) view;
        }

        ImageCardView getCardView() {
            return mCardView;
        }

        void updateCardViewImage(Context context, String link) {
            if(link == null || link.isEmpty()) {
                mCardView.setMainImage(context.getDrawable(R.drawable.recording_unkown));
                return;
            }

            GlideApp.with(context)
            .load(link)
            .override(MoviePresenter.WIDTH, MoviePresenter.HEIGHT)
            .centerCrop()
            .error(context.getDrawable(R.drawable.recording_unkown))
            .placeholder(context.getDrawable(R.drawable.recording_unkown))
            .into(mCardView.getMainImageView());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new MovieImageCardView(parent.getContext(), true);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ArtworkHolder holder = (ArtworkHolder) item;
        ViewHolder vh = (ViewHolder) viewHolder;

        MovieImageCardView cardView = (MovieImageCardView)vh.getCardView();
        Context context = cardView.getContext();

        cardView.setTitleText(holder.getTitle());
        cardView.setMainImageDimensions(MoviePresenter.WIDTH, MoviePresenter.HEIGHT);
        cardView.setInfoAreaBackgroundColor(Utils.getColor(context, R.color.primary_color));
        cardView.setBackgroundUrl(holder.getBackgroundUrl());

        vh.updateCardViewImage(cardView.getContext(), holder.getPosterUrl());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

}
