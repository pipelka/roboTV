package org.robotv.tests.artwork;

import org.junit.Test;
import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.artwork.provider.ArtworkProvider;
import org.robotv.client.artwork.provider.HttpEpgImageProvider;
import org.robotv.client.model.Event;

import java.io.IOException;

import static org.junit.Assert.*;

public class EpgImagesTest {

    @Test
    public void testValidEventId() {
        ArtworkProvider provider = new HttpEpgImageProvider("http://www.stockvault.net/data/2011/11/04/%d/small.jpg");
        ArtworkHolder result = null;

        try {
            result = provider.search(new Event(0x15, "MyEvent", null, null, 0, 0, 127837));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(result);
        assertTrue(result.hasPoster());
        assertTrue(result.hasBackground());
    }

    @Test
    public void testInvalidEventId() {
        ArtworkProvider provider = new HttpEpgImageProvider("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/%d.jpg");
        ArtworkHolder result = null;

        try {
            result = provider.search(new Event(0x15, "MyEvent", null, null, 0, 127837));
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        assertNull(result);
    }

    @Test
    public void testInvalidUrl() {
        ArtworkProvider provider = new HttpEpgImageProvider("http://www.invalidurl.xxx/data/%d.jpg");
        ArtworkHolder result;

        try {
            result = provider.search(new Event(0x15, "MyEvent", null, null, 0, 999999999));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        assertNull(result);
    }

    @Test
    public void testEmptyTemplate() {
        ArtworkProvider provider = new HttpEpgImageProvider("");
        ArtworkHolder result;

        try {
            result = provider.search(new Event(0x15, "MyEvent", null, null, 0, 999999999));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        assertNull(result);
    }

    @Test
    public void testInvalidTemplate() {
        ArtworkProvider provider = new HttpEpgImageProvider("https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/%i.jpg");
        ArtworkHolder result;

        try {
            result = provider.search(new Event(0x15, "MyEvent", null, null, 0, 999999999));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        assertNull(result);
    }
}
