package org.xvdr.player;

import com.google.android.exoplayer2.C;

public class PositionReference {

    private long timeUs = 0;

    private long startPosition;
    private long endPosition;
    private long currentPosition;

    PositionReference() {
        reset();
    }

    public void reset() {
        startPosition = System.currentTimeMillis();
        endPosition = C.TIME_UNSET;
        currentPosition = startPosition;
    }

    public void set(long timeUs, long wallClockTime) {
        this.timeUs = timeUs;
        this.currentPosition = wallClockTime;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long pos) {
        startPosition = pos;
    }

    long getDuration() {
        if(endPosition == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }

        return endPosition - startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(long pos) {
        endPosition = pos;
    }

    public long positionFromTimeUs(long timeUs) {
        long diffMs = (timeUs - this.timeUs) / 1000;
        return currentPosition + diffMs;
    }

    public long timeUsFromPosition(long position) {
        return timeUs + (position - currentPosition) * 1000;
    }

}
