package org.xvdr.extractor;

public class PlaybackSpeedAdjuster {

    private int mSpeed = 1;
    private long mStartTimeUs = 0;
    private long mOffsetUs = 0;

    void setSpeed(int speed, long timeUs) {
        mSpeed = speed;
        mStartTimeUs = timeUs;
        mOffsetUs = -1;
    }

    int getSpeed() {
        return mSpeed;
    }

    public long adjustTimestamp(long timeUs) {
        if(mOffsetUs == -1) {
            mOffsetUs = timeUs - mStartTimeUs;
        }

        timeUs -= mOffsetUs;

        if(timeUs > mStartTimeUs) {
            long diff = (timeUs - mStartTimeUs) / mSpeed;
            timeUs = mStartTimeUs + diff;
        }

        return timeUs;
    }

    public void seek(long timeUs) {
        mOffsetUs = -1;
        mStartTimeUs = 0;
    }
}
