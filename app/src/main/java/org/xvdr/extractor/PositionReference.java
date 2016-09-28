package org.xvdr.extractor;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.ts.TimestampAdjuster;

class PositionReference {

    private long timeUs = 0;

    private long startPosition;
    private long endPosition;
    private long currentPosition;

    final private TimestampAdjuster timestampAdjuster = new TimestampAdjuster(TimestampAdjuster.DO_NOT_OFFSET);

    PositionReference() {
        reset();
    }

    void reset() {
        startPosition = System.currentTimeMillis();
        endPosition = C.TIME_UNSET;
        currentPosition = startPosition;
    }
    void set(long timeUs, long wallClockTime) {
        this.timeUs = timeUs;
        this.currentPosition = wallClockTime;
    }

    long getStartPosition() {
        return startPosition;
    }

    void setStartPosition(long pos) {
        // start position is monotonic increasing (and behind current position)
        if(pos > currentPosition || pos <= startPosition) {
            return;
        }
        startPosition = pos;
    }

    long getDuration() {
        if(endPosition == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }

        return endPosition - startPosition;
    }

    long getEndPosition() {
        return endPosition;
    }

    void setEndPosition(long pos) {
        endPosition = pos;
    }

    long getCurrentPosition() {
        return currentPosition;
    }

    long positionFromTimeUs(long timeUs) {
        long diffMs = (timeUs - this.timeUs) / 1000;
        return currentPosition + diffMs;
    }

    long timeUsFromPosition(long position) {
        return timeUs + (position - currentPosition) * 1000;
    }

    long adjustTimestamp(long ts) {
        return timestampAdjuster.adjustTsTimestamp(ts);
    }

    void resetTimestamp() {
        timestampAdjuster.reset();
    }
}
