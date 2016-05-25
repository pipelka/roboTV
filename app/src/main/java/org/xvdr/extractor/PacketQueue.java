package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;

import java.util.ArrayDeque;

public class PacketQueue  {

    final private static String TAG = "PacketQueue";

    private AdaptiveAllocator mAllocator;
    private ArrayDeque<Allocation> mQueue;

    private MediaFormat mFormat;

    private long mLargestTimestampUs = 0;
    private long mSmallestTimestampUs = 0;

    public PacketQueue(int bufferCount, int bufferSize) {
        mAllocator = new AdaptiveAllocator(bufferCount, bufferSize);
        mQueue = new ArrayDeque<>(bufferCount);
    }

    synchronized public void format(MediaFormat format) {
        Allocation holder = new Allocation(format);
        mFormat = format;

        mQueue.add(holder);
    }

    synchronized public void sampleData(Allocation buffer) {
        mLargestTimestampUs = Math.max(mLargestTimestampUs, buffer.timeUs);

        if(mSmallestTimestampUs == 0) {
            mSmallestTimestampUs = mLargestTimestampUs;
        }

        mQueue.add(buffer);
    }

    public MediaFormat getFormat() {
        return mFormat;
    }

    public boolean hasFormat() {
        return (mFormat != null);
    }

    public long bufferSizeMs() {
        return (mLargestTimestampUs - mSmallestTimestampUs) / 1000;
    }

    public long getBufferedPositionUs() {
        return mLargestTimestampUs;
    }

    private Allocation poll() {
        Allocation a = mQueue.poll();
        mAllocator.release(a);
        return a;
    }

    synchronized public boolean readFormat(MediaFormatHolder formatHolder) {
        Allocation a = mQueue.peek();

        if(a == null || !a.isFormat()) {
            return false;
        }

        formatHolder.format = a.getFormat();
        poll();

        return true;
    }

    synchronized public boolean readSample(SampleHolder sampleHolder) {
        Allocation a = mQueue.peek();

        if(a == null || !a.isSample()) {
            return false;
        }

        sampleHolder.flags = a.flags;
        sampleHolder.timeUs = a.timeUs;
        sampleHolder.size = a.length();
        sampleHolder.ensureSpaceForWrite(sampleHolder.size);
        sampleHolder.data.put(a.data(), 0, sampleHolder.size);

        mSmallestTimestampUs = Math.max(mSmallestTimestampUs, sampleHolder.timeUs);
        poll();

        return true;
    }

    synchronized public Allocation allocate(int bufferSize) {
        return mAllocator.allocate(bufferSize);
    }

    synchronized void release(Allocation a) {
        mAllocator.release(a);
    }

    synchronized public boolean isEmpty() {
        return (mQueue.peek() == null);
    }

    synchronized public void clear() {
        mQueue.clear();
        mAllocator.releaseAll();
        mLargestTimestampUs = 0;
        mSmallestTimestampUs = 0;
    }
}
