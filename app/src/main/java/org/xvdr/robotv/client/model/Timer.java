package org.xvdr.robotv.client.model;

public class Timer extends Event {

    private int id;
    private int flags;
    private int priority;
    private int lifeTime;
    private long timerStartTime;
    private long timerEndTime;
    private int searchTimerId;
    private int recordingId;
    private String logoUrl;
    private String posterUrl;

    public Timer(int id, Event event) {
        super(event);
        this.id = id;
        this.timerStartTime = event.getStartTime();
        this.timerEndTime = event.getStartTime() + event.getDuration();
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
        return timerStartTime;
    }

    public void setTimerStartTime(long timerStartTime) {
        this.timerStartTime = timerStartTime;
    }

    public long getTimerEndTime() {
        return timerEndTime;
    }

    public void setTimerEndTime(long timerEndTime) {
        this.timerEndTime = timerEndTime;
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
}
