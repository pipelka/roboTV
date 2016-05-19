package org.xvdr.extractor;

import android.support.v4.util.Pools;

import com.google.android.exoplayer.MediaFormat;

import java.util.concurrent.ArrayBlockingQueue;

public class PacketQueue extends ArrayBlockingQueue<Allocation> {

    final private static String TAG = "PacketQueue";
    private int mQueueSize = 400;

    private MediaFormat mFormat;

    public PacketQueue(int queueSize) {
        super(queueSize);
        mQueueSize = queueSize;
    }

    public void format(MediaFormat format) {
        Allocation holder = new Allocation(format);
        mFormat = format;

        try {
            put(holder);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void sampleData(Allocation buffer) {
        try {
            put(buffer);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MediaFormat getFormat() {
        return mFormat;
    }

    public boolean hasFormat() {
        return (mFormat != null);
    }

    public boolean isFull() {
        return (size() >= mQueueSize);
    }
}
