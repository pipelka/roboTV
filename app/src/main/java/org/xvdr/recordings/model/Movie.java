package org.xvdr.recordings.model;

import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.Event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Movie extends Event {

    private String folder;
    private String posterUrl;
    private String backgroundUrl;
    private long timeStamp;
    private String recordingId;
    private String channelName;
    private int channelUid;
    private boolean isSeriesHeader = false;
    private int episodeCount;
    private int playCount;

    public Movie(int contentId, String title, String subTitle, String plot, int durationSec) {
        super(contentId, title, subTitle, plot, durationSec);
    }

    public Movie(Event event) {
        super(
                event.getContentId(),
                event.getTitle(),
                event.getShortText(),
                event.getDescription(),
                event.getDuration(),
                event.getEventId(),
                event.getChannelUid()
        );
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String category) {
        this.folder = category;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String cardImageUrl) {
        this.posterUrl = cardImageUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundImageUrl) {
        this.backgroundUrl = backgroundImageUrl;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getDurationMs() {
        return getDuration() * 1000;
    }

    void setRecordingId(String id) {
        this.recordingId = id;
    }

    public String getRecordingId() {
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

    public void setChannelUid(int channelUid) {
        this.channelUid = channelUid;
    }

    public int getChannelUid() {
        return channelUid;
    }

    public String getDate() {
        return DateFormat.getDateInstance().format(new Date(timeStamp));
    }

    public String getDateTime() {
        return DateFormat.getDateTimeInstance().format(new Date(timeStamp));
    }

    void setPlayCount(int count) {
        playCount = count;
    }

    public int getPlayCount() {
        return playCount;
    }

    void setSeriesHeader() {
        isSeriesHeader = true;
    }

    public boolean isSeriesHeader() {
        return isSeriesHeader;
    }

    public void setArtwork(ArtworkHolder artwork) {
        if(artwork == null) {
            return;
        }

        posterUrl = artwork.getPosterUrl();
        backgroundUrl = artwork.getBackgroundUrl();
    }

    public void setArtwork(Movie movie) {
        posterUrl = movie.getPosterUrl();
        backgroundUrl = movie.getBackgroundUrl();
    }

    @Override
    public String toString() {
        return "Movie {" +
               "folder=\'" + folder + "\'" +
               ", posterUrl=\'" + posterUrl + "\'" +
               ", backgroundUrl=\'" + backgroundUrl + "\'" +
               ", episodeCount=\'" + episodeCount + "\'" +
               ", timeStamp=\'" + timeStamp + "\'" +
               ", recordingId=\'" + recordingId + "\'" +
               ", channelUid=\'" + channelUid + "\'" +
               ", channelName=\'" + channelName + "\'" +
               ", playCount=\'" + playCount + "\'" +
               "}";
    }

    public void setStartTime(long startTime) {
        this.timeStamp = startTime * 1000;
    }

    public long getStartTime() {
        return timeStamp / 1000;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    void setEpisodeCount(int count) {
        episodeCount = count;
    }
}
