package org.robotv.client.artwork.provider;

import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.model.Event;

import java.io.IOException;

abstract class SimpleArtworkProvider extends HttpArtworkProvider {
    @Override
    protected ArtworkHolder searchMovie(Event event) throws IOException {
        return null;
    }

    @Override
    protected ArtworkHolder searchTv(Event event) throws IOException {
        return null;
    }

    public abstract ArtworkHolder search(Event event) throws IOException;
}
