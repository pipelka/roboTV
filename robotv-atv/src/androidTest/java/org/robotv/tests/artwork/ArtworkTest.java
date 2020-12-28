package org.robotv.tests.artwork;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robotv.client.Connection;
import org.robotv.client.MovieController;
import org.robotv.client.PacketAdapter;
import org.robotv.client.artwork.ArtworkFetcher;
import org.robotv.client.model.Event;
import org.robotv.msgexchange.Packet;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ArtworkTest {

    static final private String TAG = "EpgSearchTest";

    final Connection mConnection;

    public ArtworkTest() {
        mConnection = new Connection("artworkTest", "deu");
        assertTrue(mConnection.open("192.168.16.153"));
    }

    @Test
    public void testSetArtwork() {
        ArtworkFetcher artworkFetcher = new ArtworkFetcher(mConnection, "deu");
        Event event = new Event(
            21,
            "",
            null,
            null,
            0,
            0,
            54882,
            2052563304,
            "http://image.tmdb.org/t/p/w1280/suopoADq0k8YZr4dQXcU6pToj6s.jpg",
            "http://image.tmdb.org/t/p/w1280/suopoADq0k8YZr4dQXcU6pToj6s.jpg"
        );

        assertTrue(artworkFetcher.putForEvent(event));
    }
}
