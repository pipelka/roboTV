package org.xvdr.recordings.presenter;

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import org.xvdr.recordings.model.Movie;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;

        if(movie != null) {
            viewHolder.getTitle().setText(movie.getTitle());
            viewHolder.getSubtitle().setText(movie.getOutline());
            viewHolder.getBody().setText(movie.getDescription());
        }
    }
}
