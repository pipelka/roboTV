package org.xvdr.robotv.client;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.client.model.Event;
import org.xvdr.robotv.client.model.Movie;
import org.xvdr.robotv.client.model.Timer;

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
        movie.setRecordingId(Integer.parseInt(recId, 16));
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

    public static Timer toTimer(Packet response) {

        int id = (int) response.getU32();               // id
        int flags = (int) response.getU32();            // timer flags
        int priority = (int) response.getU32();         // timer priority
        int lifeTime = (int) response.getU32();         // lifetime
        int channelUid = (int) response.getU32();       // channel uid
        int startTime = (int) response.getU32();        // timer start timer
        int stopTime = (int) response.getU32();         // timer stop time
        int searchTimerId = (int) response.getU32();    // search timer id
        int recordingId = (int) response.getU32();      // id of recording
        String logoUrl = response.getString();          // logo url

        boolean hasEvent = (response.getU8() == 1);
        Event event;

        if(hasEvent) {
            event = PacketAdapter.toEvent(response);
        }
        else {
            event = new Event(0, "", "", "", stopTime - startTime, 0, channelUid);
            event.setStartTime(startTime);
        }

        Timer timer = new Timer(id, event);
        timer.setFlags(flags);
        timer.setPriority(priority);
        timer.setLifeTime(lifeTime);
        timer.setTimerStartTime(startTime);
        timer.setTimerEndTime(stopTime);
        timer.setSearchTimerId(searchTimerId);
        timer.setRecordingId(recordingId);
        timer.setLogoUrl(logoUrl);
        timer.setChannelUid(channelUid);

        return timer;
    }

    public static Timer toSearchTimer(Packet response) {

        int id = (int) response.getU32();                   // id
        String title = response.getString();                // search term (title)
        int channelUid = (int) response.getU32();           // channel uid
        String channelName = response.getString();          // channel name
        boolean seriesRecording = (response.getU32() == 1); // is series recording
        String folder = response.getString();               // folder
        String logoUrl = response.getString();              // channel logo url
        String definition = response.getString();           // epgsearch timer description

        int contentId = seriesRecording ? 0x15 : 0x10;
        Timer timer = new Timer(id, new Event(contentId, title, "", "", 0));
        timer.setChannelUid(channelUid);
        timer.setSearchTimerId(id);
        timer.setLogoUrl(logoUrl);

        return timer;
    }
}
