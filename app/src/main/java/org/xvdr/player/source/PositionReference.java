package org.xvdr.player.source;

public class PositionReference {

    private long timeUs = 0;

    private long startPosition;
    private long endPosition;
    private long currentPosition;

    public PositionReference() {
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

    void setStartPosition(long pos) {
        if(!isPositionSane(pos)) {
            return;
        }

        startPosition = pos;
    }

    public long getDuration() {
        return endPosition - startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }

    void setEndPosition(long pos) {
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
        long saneWindowMs = 3 * 60 * 60 * 1000;
        return (Math.abs(currentPosition - pos) <= saneWindowMs);
    }

    public long timeUsFromPosition(long position) {
        return timeUs + (position - currentPosition) * 1000;
    }

}
