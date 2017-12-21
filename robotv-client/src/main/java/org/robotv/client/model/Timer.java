package org.robotv.client.model;

import android.text.TextUtils;

public class Timer extends Event {

    private int id;
    private int flags;
    private int priority;
    private int lifeTime;
    private int searchTimerId;
    private int recordingId;
    private String logoUrl;
    private String posterUrl;
    private String folder;

    static public int preStartRecording = 2 * 60;
    static public int postEndRecording = 5 * 60;

    public Timer(int id, Event event) {
        super(event);
        this.id = id;
        this.flags = 0;
        this.searchTimerId = -1;
        this.recordingId = 0;
    }

    public int getId() {
        return id;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public long getTimerStartTime() {
        if(getVpsTime() > 0) {
            return getVpsTime();
        }

        return getStartTime() - preStartRecording;
    }

    public long getTimerEndTime() {
        if(getVpsTime() > 0) {
            return getVpsTime() + getDuration();
        }

        return getStartTime() + getDuration() + postEndRecording;
    }

    public int getSearchTimerId() {
        return searchTimerId;
    }

    public void setSearchTimerId(int searchTimerId) {
        this.searchTimerId = searchTimerId;
    }

    public int getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(int recordingId) {
        this.recordingId = recordingId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public boolean isSearchTimer() {
        return (searchTimerId != -1);
    }

    public boolean isRecording() {
        return (flags & 8) == 8;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Timer)) {
            return false;
        }

        Timer timer = (Timer) o;
        return timer.getStartTime() == getStartTime() &&
            timer.getTitle().equals(getTitle()) &&
            timer.getId() == getId();
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        if(TextUtils.isEmpty(folder)) {
            folder = "";
        }

        int i = folder.indexOf('/');

        if (i > 0) {
            folder = folder.substring(0, i);
        }

        this.folder = folder;
    }

    public String getRecordingName() {
        String name;

        if(TextUtils.isEmpty(getFolder())) {
            name = getTitle();
        }
        else {
            name = getFolder() + "~" + getTitle();
        }

        if(isTvShow()) {
            name += "~" + getShortText();
        }

        return name.replace(' ', '_').replace(':', '_');
    }
}
