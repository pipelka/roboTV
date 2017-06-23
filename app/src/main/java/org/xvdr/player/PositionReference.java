package org.xvdr.player;

import com.google.android.exoplayer2.C;

public class PositionReference {

    private long timeUs = 0;

    private long startPosition;
    private long endPosition;
    private long currentPosition;

    static private long saneWindowMs = 3 * 60 * 60 * 1000;

    PositionReference() {
        reset();
    }

    public void reset() {
        startPosition = System.currentTimeMillis();
        endPosition = startPosition;
        currentPosition = startPosition;
    }

    public void set(long timeUs, long wallClockTime) {
        this.timeUs = timeUs;

        if(isPositionSane(wallClockTime)) {
            this.currentPosition = wallClockTime;
        }
    }

    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long pos) {
        if(!isPositionSane(pos)) {
            return;
        }

        startPosition = pos;
    }

    long getDuration() {
        return endPosition - startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(long pos) {
        if(!isPositionSane(pos)) {
            return;
        }

        endPosition = pos;
    }

    public long positionFromTimeUs(long timeUs) {
        long diffMs = (timeUs - this.timeUs) / 1000;
        return currentPosition + diffMs;
    }

    private boolean isPositionSane(long pos) {
        return (Math.abs(currentPosition - pos) <= saneWindowMs);
    }

    long timeUsFromPosition(long position) {
        return timeUs + (position - currentPosition) * 1000;
    }

}
