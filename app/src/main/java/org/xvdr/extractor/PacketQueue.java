package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;

import java.util.concurrent.LinkedBlockingQueue;

public class PacketQueue extends LinkedBlockingQueue<PacketQueue.PacketHolder> {

    private MediaFormat mFormat;
    private int mMaxQueueSize = 300;

    public class PacketHolder {
        public byte[] data = null;
        public MediaFormat format = null;
        public long timeUs;
        public int flags;
        public int length;

        public PacketHolder(MediaFormat format) {
            this.format = format;
        }

        public PacketHolder(byte[] data, int length, long timeUs, int flags) {
            this.data = data;
            this.length = length;
            this.timeUs = timeUs;
            this.flags = flags;
        }

        public boolean isFormat() {
            return (format != null);
        }

        public boolean isSample() {
            return (data != null);
        }

    }

    public void format(MediaFormat format) {
        PacketHolder holder = new PacketHolder(format);
        mFormat = format;

        try {
            put(holder);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sampleData(byte[] data, int length, long timeUs, int flags) {
        PacketHolder holder = new PacketHolder(data, length, timeUs, flags);

        if(size() >= mMaxQueueSize) {
            return;
        }

        try {
            put(holder);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MediaFormat getFormat() {
        return mFormat;
    }

    public boolean hasFormat() {
        return (mFormat != null);
    }
}
