package org.xvdr.robotv.client;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.client.model.Event;
import org.xvdr.robotv.client.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class PacketAdapter {

    public static Movie toMovie(Packet p) {

        long startTime = p.getU32();            // start time
        int duration = (int)p.getU32();         // duration
        p.getU32();                             // Priority
        p.getU32();                             // Lifetime
        String channelName = p.getString();     // channel name
        String title = p.getString();           // title
        String outline = p.getString();         // outline
        String plot = p.getString();            // plot
        String folder = p.getString();          // folder
        String recId = p.getString();           // recording id
        int playCount = (int)p.getU32();        // playcount
        int content = (int)p.getU32();          // content
        String posterUrl = p.getString();       // poster url
        String backgroundUrl = p.getString();   // background url

        Movie movie = new Movie(content, title, outline, plot, duration);

        if(title.equals(outline) || outline.isEmpty()) {
            movie.setShortText(movie.getDate());
        }

        movie.setStartTime(startTime);
        movie.setFolder(folder); // directory / folder
        movie.setChannelName(channelName);
        movie.setRecordingId(recId);
        movie.setPlayCount(playCount);
        movie.setPosterUrl(posterUrl);
        movie.setBackgroundUrl(backgroundUrl);

        return movie;
    }

    public static Event toEvent(Packet p) {
        final int eventId = (int) p.getU32();
        long startTime = p.getU32();
        final int duration = (int) p.getU32();
        int contentId;
        List<Integer> list = new ArrayList<>();

        while((contentId = (int) p.getU8()) != 0) {
            list.add(contentId);
        }

        if(list.size() != 0) {
            contentId = list.get(0);
        }

        p.getU32(); // rating
        String title = p.getString();
        String shortText = p.getString();
        String description = p.getString();

        Event e = new Event(contentId, title, shortText, description, duration, eventId);
        e.setStartTime(startTime);

        return e;
    }

}
