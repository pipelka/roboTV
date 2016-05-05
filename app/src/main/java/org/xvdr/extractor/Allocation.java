package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;

public class Allocation {

    private int mSize = 0;
    private byte[] mData;
    private boolean mAllocated = false;
    private int mLength = 0;
    private MediaFormat mFormat = null;

    public long timeUs;
    public int flags;

    public Allocation(int size) {
        resize(size);
    }

    public Allocation(MediaFormat format) {
        mFormat = format;
    }

    public boolean isFormat() {
        return (mFormat != null);
    }

    public boolean isSample() {
        return (mFormat == null);
    }

    public void resize(int size) {
        mSize = size;
        mData = new byte[mSize];
    }

    MediaFormat getFormat() {
        return mFormat;
    }

    public byte[] data() {
        return mData;
    }

    public void setLength(int length) {
        mLength = length;
    }

    public int length() {
        return mLength;
    }

    public int size() {
        return mSize;
    }

    synchronized public boolean allocate() {
        if(mAllocated) {
            return false;
        }

        mAllocated = true;
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
