package org.robotv.client;

import android.text.TextUtils;

import org.robotv.client.model.Movie;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

public class RelatedContentExtractor {

    private final ArrayList<Movie> collection;

    public RelatedContentExtractor(ArrayList<Movie> collection) {
        this.collection = collection;
    }

    private final ArrayList<Movie> result = new ArrayList<>(50);

    public ArrayList<Movie> getSeries(String title) {
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

    ArrayList<Movie> getRelatedMovies(Movie movie) {
        if(collection == null) {
            return result;
        }

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
