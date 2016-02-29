package org.xvdr.extractor;

public class Allocation {

    private int mSize;
    private byte[] mData;
    private boolean mAllocated = false;
    private int mLength = 0;

    public Allocation(int size) {
        resize(size);
    }

    public void resize(int size) {
        mSize = size;
        mData = new byte[mSize];
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
        return true;
    }

}
