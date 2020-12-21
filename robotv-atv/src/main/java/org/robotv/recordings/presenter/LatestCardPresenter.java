package org.robotv.recordings.presenter;

import android.content.Context;

import androidx.leanback.widget.Presenter;

import org.robotv.client.Connection;
import org.robotv.client.model.Movie;
import org.robotv.recordings.model.MovieImageCardView;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;

public class LatestCardPresenter extends MoviePresenter {

    public LatestCardPresenter(Connection connection, boolean changeBackground) {
        super(connection, changeBackground);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        MoviePresenter.ViewHolder vh = (ViewHolder) viewHolder;
        MovieImageCardView cardView = vh.getCardView();
        Context context = cardView.getContext();

        String contextText = movie.getDate() + " " + movie.getTime();

        cardView.setTitleText(movie.getTitle());
        cardView.setContentText(contextText);
        cardView.setMainImageDimensions(MoviePresenter.WIDTH, MoviePresenter.HEIGHT);
        cardView.setInfoAreaBackgroundColor(Utils.getColor(context, R.color.primary_color));

        cardView.setOnLongClickListener(v -> {
            if(listener == null) {
                return false;
            }

            return listener.onLongClick(movie);
        });

        vh.update(movie, connection, context);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.getCardView().setOnLongClickListener(null);
    }
}
