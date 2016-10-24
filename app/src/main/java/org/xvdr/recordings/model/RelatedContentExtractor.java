package org.xvdr.recordings.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class RelatedContentExtractor {

    private Collection<Movie> collection;

    public RelatedContentExtractor(Collection<Movie> collection) {
        this.collection = collection;
    }

    Collection<Movie> result = new ArrayList<>(50);

    public Collection<Movie> getSeries(String name) {
        Iterator<Movie> i = collection.iterator();
        while(i.hasNext()) {
            Movie m = i.next();
            if(m.isSeries() && m.getCategory().equals(name)) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }

    public Collection<Movie> getRelatedMovies(Movie movie) {
        Iterator<Movie> i = collection.iterator();
        while(i.hasNext()) {
            Movie m = i.next();

            if(m.getId().equals(movie.getId())) {
                continue;
            }

            if(m.getCategory().equals(movie.getCategory()) && m.getContent() == movie.getContent()) {
                result.add(m);
            }
        }

        if(result.size() == 0) {
            return null;
        }

        return result;
    }
}
