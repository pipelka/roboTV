package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;

import java.util.concurrent.LinkedBlockingQueue;

public class PacketQueue extends LinkedBlockingQueue<PacketQueue.PacketHolder> {

    final private static String TAG = "PacketQueue";
    final private static int mMaxQueueSize = 400;

    private MediaFormat mFormat;

    public class PacketHolder {
        public Allocation buffer = null;
        public MediaFormat format = null;
        public long timeUs;
        public int flags;

        public PacketHolder(MediaFormat format) {
            this.format = format;
        }

        public PacketHolder(Allocation buffer, long timeUs, int flags) {
            this.buffer = buffer;
            this.timeUs = timeUs;
            this.flags = flags;
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

    public PacketQueue() {
        super(mMaxQueueSize);
    }

    public void format(MediaFormat format) {
        PacketHolder holder = new PacketHolder(format);
        mFormat = format;

        try {
            put(holder);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void sampleData(Allocation buffer, long timeUs, int flags) {
        PacketHolder holder = new PacketHolder(buffer, timeUs, flags);

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
        return (size() >= mMaxQueueSize);
    }

    synchronized public void clear() {
        for(PacketHolder item : this) {
            item.release();
        }

        super.clear();
    }
}
