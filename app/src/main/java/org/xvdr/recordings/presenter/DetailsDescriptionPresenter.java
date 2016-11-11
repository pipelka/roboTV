package org.xvdr.recordings.presenter;

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import org.xvdr.robotv.client.model.Movie;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;

        if(movie == null) {
            return;
        }

        if(movie.isTvShow()) {
            viewHolder.getTitle().setText(movie.getShortText());
            viewHolder.getBody().setText(movie.getDescription());
            return;
        }

        viewHolder.getTitle().setText(movie.getTitle());
        viewHolder.getSubtitle().setText(movie.getShortText());
        viewHolder.getBody().setText(movie.getDescription());
    }
}
