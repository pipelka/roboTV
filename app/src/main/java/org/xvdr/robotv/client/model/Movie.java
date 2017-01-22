package org.xvdr.robotv.client.model;

import org.xvdr.robotv.artwork.ArtworkHolder;

public class Movie extends Event {

    private String folder;
    private int recordingId;
    private String channelName;
    private boolean isSeriesHeader = false;
    private int episodeCount;
    private int playCount;

    public Movie(int contentId, String title, String subTitle, String plot, int durationSec) {
        super(contentId, title, subTitle, plot, 0, durationSec);
    }

    public Movie(int contentId, String title, String subTitle, String plot, long startTime, int durationSec) {
        super(contentId, title, subTitle, plot, startTime, durationSec);
    }

    public Movie(Event event) {
        super(event);
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String category) {
        this.folder = category;
    }

    public long getDurationMs() {
        return getDuration() * 1000;
    }

    public void setRecordingId(int id) {
        this.recordingId = id;
    }

    public int getRecordingId() {
        return recordingId;
    }

    public int getGenreType() {
        return getContentId() & 0xF0;
    }

    public int getGenreSubType() {
        return getContentId() & 0x0F;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setPlayCount(int count) {
        playCount = count;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setSeriesHeader() {
        isSeriesHeader = true;
    }

    public boolean isSeriesHeader() {
        return isSeriesHeader;
    }

    public void setArtwork(ArtworkHolder artwork) {
        if(artwork == null) {
            return;
        }

        setPosterUrl(artwork.getPosterUrl());
        setBackgroundUrl(artwork.getBackgroundUrl());
    }

    public void setArtwork(Movie movie) {
        setPosterUrl(movie.getPosterUrl());
        setBackgroundUrl(movie.getBackgroundUrl());
    }

    @Override
    public String toString() {
        return "Movie {" +
               "folder=\'" + folder + "\'" +
               ", episodeCount=\'" + episodeCount + "\'" +
               ", recordingId=\'" + recordingId + "\'" +
               ", channelName=\'" + channelName + "\'" +
               ", playCount=\'" + playCount + "\'" +
               "}";
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int count) {
        episodeCount = count;
    }

    public String getRecordingIdString() {
        return Integer.toString(recordingId, 16);
    }
}
