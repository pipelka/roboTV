package org.xvdr.recordings.model;

import org.xvdr.jniwrap.Packet;

public class PacketAdapter {

    public static Movie toMovie(Packet p) {
        Movie movie = new Movie();

        movie.setTimeStamp(p.getU32() * 1000L);
        movie.setDuration((int)p.getU32());
        p.getU32(); // Priority
        p.getU32(); // Lifetime
        movie.setChannelName(p.getString()); // ChannelName

        String title = p.getString();
        String outline = p.getString();

        if(title.equals(outline) || outline.isEmpty()) {
            outline = movie.getDate();
        }

        movie.setTitle(title);
        movie.setOutline(outline); // plot outline
        movie.setDescription(p.getString());

        // folder
        String folder = p.getString();

        movie.setCategory(folder); // directory / folder
        movie.setId(p.getString());
        p.getU32(); // playcount

        movie.setContent((int)p.getU32()); // content

        movie.setCardImageUrl(p.getString()); // thumbnail url
        movie.setBackgroundImageUrl(p.getString()); // icon url

        return movie;
    }
}
