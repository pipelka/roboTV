package org.robotv.tests.artwork;

import org.junit.Test;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.provider.TheMovieDatabase;
import org.robotv.client.model.Event;

import static org.junit.Assert.*;

import java.io.IOException;

public class MovieDatabaseTest{

    private final static String TMDB_APIKEY = "958abef9265db99029a13521fddcb648";

    TheMovieDatabase tmdb = new TheMovieDatabase(TMDB_APIKEY, "de");

    @Test
    public void testSeries() {
        ArtworkHolder r = null;

        try {
            r = tmdb.search(new Event(0x15, "Criminal Minds", null, null, 0, 45 * 60));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(r);
        assertTrue(r.hasPoster());
        assertTrue(r.hasBackground());
    }

    @Test
    public void testMovie() {
        ArtworkHolder r = null;

        try {
            r = tmdb.search(new Event(0x10, "In tödlicher Mission", null, null, 0, 120 * 60));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(r);
        assertTrue(r.hasPoster());
        assertTrue(r.hasBackground());
    }

    @Test
    public void testMovieYear() {
        ArtworkHolder r = null;

        try {
            r = tmdb.search(new Event(0x10, "Der Tag, an dem die Erde stillstand", "Science-Fiction, USA 1951", null, 0, 120 * 60));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(r);
        assertTrue(r.hasPoster());
        assertTrue(r.hasBackground());

        //assertEquals("http://image.tmdb.org/t/p/w1280/gdbiItlBZ5Fdr5tRnlrozkbKknW.jpg", r.getBackgroundUrl());

        try {
            r = tmdb.search(new Event(0x10, "Der Tag, an dem die Erde stillstand", "Science-Fiction, USA 2008", null, 0, 120 * 60));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(r);
        assertTrue(r.hasPoster());
        assertTrue(r.hasBackground());

        //assertEquals("http://image.tmdb.org/t/p/w600/bfRgom0djZB8z7qbtjM542j3xrT.jpg", r.getBackgroundUrl());
    }
}
