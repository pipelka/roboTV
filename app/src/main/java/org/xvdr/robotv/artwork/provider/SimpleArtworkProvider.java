package org.xvdr.robotv.artwork.provider;

import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.Event;

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
