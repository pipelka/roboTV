package org.robotv.recordings.presenter;

import android.content.Context;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import org.robotv.client.Connection;
import org.robotv.client.model.Movie;
import org.robotv.recordings.util.Utils;
import org.robotv.robotv.R;

public class LatestCardPresenter extends MoviePresenter {

    public LatestCardPresenter(Connection connection) {
        super(connection);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        MoviePresenter.ViewHolder vh = (ViewHolder) viewHolder;
        ImageCardView cardView = vh.getCardView();
        Context context = cardView.getContext();

        String contextText = movie.getDate() + " " + movie.getTime();

        cardView.setTitleText(movie.getTitle());
        cardView.setContentText(contextText);
        cardView.setMainImageDimensions(266, 400);
        cardView.setInfoAreaBackgroundColor(Utils.getColor(context, R.color.primary_color));

        vh.update(movie, connection, context);
    }

}
