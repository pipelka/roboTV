package org.xvdr.recordings.model;

import java.util.ArrayList;
import java.util.Collection;

public class RelatedContentExtractor {

    private Collection<Movie> collection;

    public RelatedContentExtractor(Collection<Movie> collection) {
        this.collection = collection;
    }

    private Collection<Movie> result = new ArrayList<>(50);

    public Collection<Movie> getSeries(String title) {
        for(Movie m : collection) {
            if (m.isSeries() && m.getTitle().equals(title)) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }

    public Collection<Movie> getRelatedMovies(Movie movie) {
        for(Movie m : collection) {
            if (m.getId().equals(movie.getId())) {
                continue;
            }

            if (m.getCategory().equals(movie.getCategory()) && m.getContent() == movie.getContent()) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }
}
