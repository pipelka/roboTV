package org.robotv.tests.artwork;

import org.junit.Test;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.provider.TheTvDb;
import org.robotv.client.model.Event;

import java.io.IOException;
import static org.junit.Assert.*;

public class TvDbTest {

    @Test
    public void testSeries() {
        TheTvDb tmdb = new TheTvDb("de");

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
}
