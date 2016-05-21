package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;

import java.util.ArrayDeque;

public class PacketQueue extends ArrayDeque<Allocation> {

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

        add(holder);
    }

    synchronized public void sampleData(Allocation buffer) {
        add(buffer);
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
