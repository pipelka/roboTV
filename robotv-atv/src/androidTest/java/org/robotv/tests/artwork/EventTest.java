package org.robotv.tests.artwork;

import org.junit.Test;
import org.robotv.client.model.Event;

import static org.junit.Assert.*;

public class EventTest {

    @Test
    public void testSeasonEpisode() {
        Event event = new Event(
                0,
                "Malcom mittendrin",
                "Episode Irgendwas",
                "5. Staffel, Folge 2: Beschreibung",
                0,
                1800
        );

        Event.SeasonEpisodeHolder episode = event.getSeasionEpisode();

        assertEquals(5, episode.season);
        assertEquals(2, episode.episode);
        assertEquals("Beschreibung", event.getDescription());
    }
}
