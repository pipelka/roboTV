package org.xvdr.extractor;

import android.support.v4.util.Pools;

import com.google.android.exoplayer.MediaFormat;

import java.util.concurrent.ArrayBlockingQueue;

public class PacketQueue extends ArrayBlockingQueue<PacketQueue.PacketHolder> {

    final private static String TAG = "PacketQueue";
    private int mQueueSize = 400;

    private MediaFormat mFormat;
    private Pools.SynchronizedPool<PacketHolder> mPool;

    public class PacketHolder {
        public Allocation buffer = null;
        public MediaFormat format = null;
        public long timeUs;
        public int flags;

        public void setFormat(MediaFormat format) {
            this.format = format;
            this.buffer = null;
        }

        public void setSample(Allocation buffer, long timeUs, int flags) {
            this.buffer = buffer;
            this.timeUs = timeUs;
            this.flags = flags;
            this.format = null;
        }

        public boolean isFormat() {
            return (format != null);
        }

        public boolean isSample() {
            return (buffer != null);
        }

        public void release() {
            if(buffer == null) {
                return;
            }

            buffer.release();
        }
    }

    public PacketQueue(int queueSize) {
        super(queueSize);
        mPool = new Pools.SynchronizedPool<>(queueSize);

        for(int i = 0; i < queueSize; i++) {
            mPool.release(new PacketHolder());
        }

        mQueueSize = queueSize;
    }

    public void format(MediaFormat format) {
        PacketHolder holder = mPool.acquire();

        holder.setFormat(format);

        mFormat = format;

        try {
            put(holder);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void sampleData(Allocation buffer, long timeUs, int flags) {
        PacketHolder holder = mPool.acquire();
        holder.setSample(buffer, timeUs, flags);

        try {
            put(holder);
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
        return (size() >= (mQueueSize * 3) / 4);
    }

    public void release(PacketHolder holder) {
        mPool.release(holder);
    }

    synchronized public void clear() {
        for(PacketHolder item : this) {
            item.release();
            release(item);
        }

        super.clear();
    }
}
