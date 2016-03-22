package org.xvdr.recordings.model;

import org.xvdr.robotv.artwork.Event;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Movie implements Serializable {
    private String title;
    private String description;
    private String outline;
    private String category;
    private String cardImageUrl;
    private String backgroundImageUrl;
    private long timeStamp;
    private int duration;
    private String id;
    private int content;
    private String channelName;
    private String formattedDate;
    private boolean isSeriesHeader = false;

    public Movie() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;

        try {
            DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date netDate = (new Date(timeStamp));
            formattedDate = sdf.format(netDate);
        }
        catch(Exception e) {
            formattedDate = "";
        }

    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setDurationMs(int duration) {
        this.duration = duration;
    }

    public long getDurationMs() {
        return duration * 1000;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setContent(int content) {
        this.content = content;
    }

    public int getContent() {
        return content;
    }

    public int getGenreType() {
        return content & 0xF0;
    }

    public int getGenreSubType() {
        return content & 0x0F;
    }

    public boolean isSeries() {
        content = Event.guessGenreFromSubTitle(content, outline, duration);
        return (content == 0x15);
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getDate() {
        return formattedDate;
    }

    public Event getEvent() {
        if(content == 0 || (content & 0x50) == 0x50) {
            content = 0x10;
        }

        return new Event(content, title, outline, description, duration);
    }

    public void setSeriesHeader() {
        isSeriesHeader = true;
    }

    public boolean isSeriesHeader() {
        return isSeriesHeader;
    }

    public void setArtwork(Movie movie) {
        cardImageUrl = movie.getCardImageUrl();
        backgroundImageUrl = movie.getBackgroundImageUrl();
    }

    @Override
    public String toString() {
        return "Movie {" +
               "title=\'" + title + "\'" +
               ", description=\'" + description + "\'" +
               ", outline=\'" + outline + "\'" +
               ", category=\'" + category + "\'" +
               ", cardImageUrl=\'" + cardImageUrl + "\'" +
               ", backgroundImageUrl=\'" + backgroundImageUrl + "\'" +
               "}";
    }
}
