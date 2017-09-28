package org.xvdr.robotv.client;

import android.text.TextUtils;

import org.xvdr.jniwrap.Packet;
import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.client.model.Channel;
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

        movie.setStartTime(startTime);
        movie.setFolder(folder); // directory / folder
        movie.setChannelName(channelName);
        movie.setRecordingId(Integer.parseInt(recId, 16));
        movie.setPlayCount(playCount);
        movie.setPosterUrl(posterUrl);
        movie.setBackgroundUrl(backgroundUrl);

        if(title.equals(outline) || outline.isEmpty()) {
            movie.setShortText(movie.getDate());
        }

        return movie;
    }

    public static Event toEvent(Packet p) {
        final int eventId = (int) p.getU32();
        long startTime = p.getU32();
        final int duration = (int) p.getU32();
        long vpsTime = p.getU32();

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

        Event e = new Event(contentId, title, shortText, description, startTime, duration, eventId);
        e.setVpsTime(vpsTime);

        return e;
    }

    public static Event toEpgEvent(Packet p) {
        // V7 EPG Information

        int eventId = (int)p.getU32();
        long startTime = p.getU32();
        long endTime = startTime + p.getU32();
        int contentId = (int)p.getU32();
        int duration = (int)(endTime - startTime);
        long parentalRating = p.getU32();
        String title = p.getString();
        String shortText = p.getString();
        String description = p.getString();

        String posterUrl = p.getString();
        String backgroundUrl = p.getString();

        // V8 extended EPG information

        long vpsTime = p.getS64();

        p.getU8(); // table id
        p.getU8(); // version
        p.getU8(); // has timer
        p.getU8(); // timer running

        // components

        long count = p.getU32();
        for(long i = 0; i < count; i++) {
            p.getString(); // description
            p.getString(); // language
            p.getU8(); // type
            p.getU8(); // stream
        }

        Event e = new Event(contentId, title, shortText, description, startTime, duration, eventId);
        e.setPosterUrl(posterUrl);
        e.setBackgroundUrl(backgroundUrl);
        e.setVpsTime(vpsTime);
        e.setParentalRating(parentalRating);

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
        String folder = response.getString();           // folder

        boolean hasEvent = (response.getU8() == 1);
        Event event;

        if(hasEvent) {
            event = PacketAdapter.toEvent(response);
        }
        else {
            event = new Event(0, "", "", "", startTime, stopTime - startTime, 0, channelUid);
        }

        Timer timer = new Timer(id, event);
        timer.setFlags(flags);
        timer.setPriority(priority);
        timer.setLifeTime(lifeTime);
        timer.setSearchTimerId(searchTimerId);
        timer.setRecordingId(recordingId);
        timer.setLogoUrl(logoUrl);
        timer.setChannelUid(channelUid);
        timer.setFolder(folder);

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
        Timer timer = new Timer(id, new Event(contentId, title, "", "", 0, 0));
        timer.setChannelUid(channelUid);
        timer.setSearchTimerId(id);
        timer.setLogoUrl(logoUrl);

        return timer;
    }

    public static Channel toChannel(Packet response) {
        Channel c = new Channel(
                (int)response.getU32(),
                response.getString(),
                (int) response.getU32(),
                (int) response.getU32(),
                response.getString(),
                response.getString());

        c.setGroupName(response.getString().trim());
        return c;
    }

    public static void toPacket(Timer timer, int priority, Packet p) {
        int flags = 1; // active
        flags |= (timer.getVpsTime() > 0) ? 4 : 0; // VPS flag

        p.putU32(timer.getId()); // index unused
        p.putU32(flags); // active timer + VPS
        p.putU32(priority); // Priority
        p.putU32(99); // Lifetime
        p.putU32(timer.getChannelUid()); // channel uid
        p.putU32(timer.getTimerStartTime()); // start time
        p.putU32(timer.getTimerEndTime()); // end time
        p.putU32(0); // day
        p.putU32(0); // weeksdays
        p.putString(timer.getRecordingName()); // recording name
        p.putString(""); // aux
    }
}
