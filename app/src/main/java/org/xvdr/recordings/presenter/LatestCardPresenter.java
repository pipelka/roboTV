package org.xvdr.recordings.presenter;

import android.graphics.drawable.ColorDrawable;
import android.support.v17.leanback.widget.Presenter;

import org.xvdr.recordings.model.Movie;

public class LatestCardPresenter extends CardPresenter {
    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        ViewHolder vh = (ViewHolder) viewHolder;

        String contextText = movie.getCategory() + (!movie.getDate().isEmpty() ? " / " + movie.getDate() : "");

        vh.getCardView().setTitleText(movie.getTitle());
        vh.getCardView().setContentText(contextText);
        vh.getCardView().setMainImageDimensions(266, 400);
        vh.updateCardViewImage(vh.getCardView().getContext(), movie.getCardImageUrl());
    }

}
