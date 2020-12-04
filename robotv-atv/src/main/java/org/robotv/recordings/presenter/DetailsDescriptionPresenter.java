package org.robotv.recordings.presenter;

import android.content.Context;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import org.robotv.robotv.R;
import org.robotv.client.model.Event;
import org.robotv.client.model.Movie;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;

        if(movie == null) {
            return;
        }

        if(movie.isTvShow()) {
            viewHolder.getTitle().setText(movie.getShortText());

            String desc = movie.getDescription();
            Event.SeasonEpisodeHolder episode = movie.getSeasionEpisode();
            if(episode.valid()) {
                Context context = viewHolder.view.getContext();
                desc += "\n";
                desc += context.getString(R.string.season_episode, episode.season, episode.episode);
            }
            viewHolder.getBody().setText(desc);
            return;
        }

        viewHolder.getTitle().setText(movie.getTitle());
        viewHolder.getSubtitle().setText(movie.getShortText());
        viewHolder.getBody().setText(movie.getDescription());
    }
}
