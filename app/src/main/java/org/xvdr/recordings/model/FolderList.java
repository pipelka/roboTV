package org.xvdr.recordings.model;

import org.xvdr.msgexchange.Packet;
import org.xvdr.robotv.client.Connection;

import java.util.TreeSet;

public class FolderList extends TreeSet<String> {

    protected FolderList() {
    }

    static public FolderList load(Connection connection) {
        String seriesFolder = connection.getConfig("SeriesFolder");

        // get movies
        Packet request = connection.CreatePacket(Connection.XVDR_RECORDINGS_GETLIST);
        Packet response = connection.transmitMessage(request);

        if(response == null) {
            return null;
        }

        response.uncompress();

        FolderList result = new FolderList();

        while(!response.eop()) {
            Movie movie = PacketAdapter.toMovie(response);
            String category = movie.getCategory();

            if(!seriesFolder.isEmpty() && category.startsWith(seriesFolder + "/")) {
                continue;
            }

            if(category.equals(PacketAdapter.FOLDER_UNSORTED)) {
                continue;
            }

            result.add(movie.getCategory());
        }

        if(!seriesFolder.isEmpty()) {
            result.add(seriesFolder);
        }

        return result;
    }

}
