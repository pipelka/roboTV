package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;

import java.nio.ByteBuffer;

public class SampleBuffer {

    private ByteBuffer mData;
    private boolean mAllocated = false;
    private MediaFormat mFormat = null;
    private static int mPartitionSize = 8192;

    public long timeUs = 0;
    public int flags = 0;
    public SampleBuffer(int size) {
        resize(size);
    }

    public SampleBuffer(MediaFormat format) {
        mFormat = format;
    }

    public boolean isFormat() {
        return (mFormat != null);
    }

    public boolean isSample() {
        return (mFormat == null);
    }

    public void resize(int size) {
        if(mData != null && mData.capacity() >= size) {
            return;
        }

        size = ((size / mPartitionSize) + 1) * mPartitionSize;

        mData = ByteBuffer.allocateDirect(size);
    }

    MediaFormat getFormat() {
        return mFormat;
    }

    public ByteBuffer data() {
        return mData;
    }

    public void setLength(int length) {
        mData.limit(length);
    }

    public int limit() {
        return mData.limit();
    }

    public int capacity() {
        return mData.capacity();
    }

    synchronized public boolean allocate() {
        if(mAllocated) {
            return false;
        }

        mAllocated = true;
        mData.clear();

        return true;
    }

    synchronized boolean release() {
        if(!mAllocated) {
            return false;
        }

        mAllocated = false;
        mFormat = null;

        return true;
    }

}
