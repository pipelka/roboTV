package org.xvdr.extractor;

import android.util.Log;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.upstream.DefaultAllocator;

import org.xvdr.extractor.upstream.RollingSampleBuffer;
import org.xvdr.jniwrap.Packet;

import java.util.ArrayDeque;

class PacketQueue {

    private class FormatHolder {

        public final MediaFormat format;
        final long timeUs;

        FormatHolder(MediaFormat format, long timeUs) {
            this.format = format;
            this.timeUs = timeUs;
        }
    }

    final private static String TAG = "PacketQueue";

    private RollingSampleBuffer rollingBuffer;
    private ArrayDeque<FormatHolder> mFormatQueue;

    private MediaFormat mFormat;

    private long mLargestTimestampUs = 0;
    private long mSmallestTimestampUs = 0;

    PacketQueue(int bufferCount, int bufferSize) {
        rollingBuffer = new RollingSampleBuffer(new DefaultAllocator(bufferSize, bufferCount));
        mFormatQueue = new ArrayDeque<>();
    }

    synchronized public void format(MediaFormat format) {
        if(!hasFormat()) {
            mFormat = format;
            return;
        }

        Log.d(TAG, "scheduled format change for: " + mLargestTimestampUs + " at: " + mSmallestTimestampUs);
        mFormat = format;
        mFormatQueue.push(new FormatHolder(format, mLargestTimestampUs));
    }

    synchronized void sampleData(Packet p, int size, long timeUs, int flags) {
        mLargestTimestampUs = Math.max(mLargestTimestampUs, timeUs);

        if(mSmallestTimestampUs == 0) {
            mSmallestTimestampUs = mLargestTimestampUs;
        }

        rollingBuffer.appendData(p, size);
        rollingBuffer.commitSample(timeUs, flags, rollingBuffer.getWritePosition() - size, size);
    }

    synchronized void sampleData(byte[] p, int size, long timeUs, int flags) {
        mLargestTimestampUs = Math.max(mLargestTimestampUs, timeUs);

        if(mSmallestTimestampUs == 0) {
            mSmallestTimestampUs = mLargestTimestampUs;
        }

        rollingBuffer.appendData(p, size);
        rollingBuffer.commitSample(timeUs, flags, rollingBuffer.getWritePosition() - size, size);
    }

    public MediaFormat getFormat() {
        return mFormat;
    }

    boolean hasFormat() {
        return (mFormat != null);
    }

    long bufferSizeMs() {
        return (mLargestTimestampUs - mSmallestTimestampUs) / 1000;
    }

    long getBufferedPositionUs() {
        return mLargestTimestampUs;
    }

    synchronized boolean readFormat(MediaFormatHolder formatHolder) {
        FormatHolder h = mFormatQueue.peek();

        if(h == null) {
            return false;
        }

        if(mSmallestTimestampUs < h.timeUs) {
            return false;
        }

        Log.d(TAG, "read format change at: " + mSmallestTimestampUs);

        mFormat = h.format;
        formatHolder.format = h.format;

        mFormatQueue.poll();
        return true;
    }

    synchronized boolean readSample(SampleHolder sampleHolder) {
        if(!rollingBuffer.readSample(sampleHolder)) {
            return false;
        }

        mSmallestTimestampUs = Math.max(mSmallestTimestampUs, sampleHolder.timeUs);
        return true;
    }

    synchronized public boolean isEmpty() {
        return rollingBuffer.isEmpty();
    }

    synchronized public void clear() {
        rollingBuffer.clear();
        mLargestTimestampUs = 0;
        mSmallestTimestampUs = 0;
    }
}
