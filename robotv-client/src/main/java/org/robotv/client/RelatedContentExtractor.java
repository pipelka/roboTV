package org.robotv.client;

import android.text.TextUtils;

import org.robotv.client.model.Movie;

import java.util.ArrayList;
import java.util.Collection;

public class RelatedContentExtractor {

    private Collection<Movie> collection;

    public RelatedContentExtractor(Collection<Movie> collection) {
        this.collection = collection;
    }

    private Collection<Movie> result = new ArrayList<>(50);

    public Collection<Movie> getSeries(String title) {
        if(collection == null || title == null) {
            return null;
        }

        for(Movie m : collection) {
            if(TextUtils.isEmpty(m.getTitle())) {
                continue;
            }

            if (m.isTvShow() && m.getTitle().equals(title)) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }

    Collection<Movie> getRelatedMovies(Movie movie) {
        for(Movie m : collection) {
            if (m.getRecordingId() == movie.getRecordingId()) {
                continue;
            }

            if (m.getFolder().equals(movie.getFolder()) && m.getContentId() == movie.getContentId()) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }
}
