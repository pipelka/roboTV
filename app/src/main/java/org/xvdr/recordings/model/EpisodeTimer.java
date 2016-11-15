package org.xvdr.recordings.model;

import org.xvdr.robotv.client.model.Timer;

import java.util.ArrayList;

public class EpisodeTimer extends Timer {

    private ArrayList<Timer> timers = new ArrayList<>();

    public EpisodeTimer(Timer timer) {
        super(0, timer);
        setSearchTimerId(-1);
        add(timer);
    }

    public boolean add(Timer timer) {
        // skip usual timer
        if(timer.getSearchTimerId() == -1) {
            return false;
        }

        // first entry ?
        if(getSearchTimerId() == -1) {
            setSearchTimerId(timer.getSearchTimerId());
            setTitle(timer.getTitle());
            setLogoUrl(timer.getLogoUrl());
            setStartTime(timer.getStartTime());
            setPosterUrl(timer.getPosterUrl());

            if(timer.isRecording()) {
                setFlags(timer.getFlags());
            }

            timers.add(timer);
            return true;
        }

        // only add matching timers
        if(getSearchTimerId() != timer.getSearchTimerId() || !getDate().equals(timer.getDate())
            || !getTitle().equals(timer.getTitle())) {
            return false;
        }

        if(timer.isRecording()) {
            setFlags(timer.getFlags());
        }

        setStartTime(Math.max(getStartTime(), timer.getStartTime()));
        timers.add(timer);

        return true;
    }

    public int getTimerCount() {
        return timers.size();
    }

    public Timer getTimer(int index) {
        return timers.get(index);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof EpisodeTimer)) {
            return false;
        }

        EpisodeTimer timer = (EpisodeTimer) o;

        return timer.getStartTime() == getStartTime() &&
            timer.getTitle().equals(getTitle()) &&
            timer.getSearchTimerId() == getSearchTimerId() &&
            timer.getLogoUrl().equals(getLogoUrl()) &&
            timer.getPosterUrl().equals(getPosterUrl()) &&
            timer.getTimerCount() == getTimerCount() &&
            timer.getFlags() == getFlags();
    }
}
