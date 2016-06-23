package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.upstream.DefaultAllocator;

import org.xvdr.extractor.upstream.RollingSampleBuffer;
import org.xvdr.msgexchange.Packet;

import java.util.ArrayDeque;

public class PacketQueue {

    protected class FormatHolder {

        public final MediaFormat format;
        public final long timeUs;

        public FormatHolder(MediaFormat format, long timeUs) {
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
    private SampleHolder mSampleHolder = new SampleHolder(SampleHolder.BUFFER_REPLACEMENT_MODE_DIRECT);

    public PacketQueue(int bufferCount, int bufferSize) {
        rollingBuffer = new RollingSampleBuffer(new DefaultAllocator(bufferSize, bufferCount));
        mFormatQueue = new ArrayDeque<>();
    }

    private long getCurrentTimeUs() {
        if(!rollingBuffer.peekSample(mSampleHolder)) {
            return 0;
        }

        return mSampleHolder.timeUs;
    }

    synchronized public void format(MediaFormat format) {
        if(!hasFormat()) {
            mFormat = format;
        }

        mFormatQueue.push(new FormatHolder(format, getCurrentTimeUs()));
    }

    synchronized public void sampleData(Packet p, int size, long timeUs, int flags) {
        mLargestTimestampUs = Math.max(mLargestTimestampUs, timeUs);

        if(mSmallestTimestampUs == 0) {
            mSmallestTimestampUs = mLargestTimestampUs;
        }

        rollingBuffer.appendData(p, size);
        rollingBuffer.commitSample(timeUs, flags, rollingBuffer.getWritePosition() - size, size);
    }

    synchronized public void sampleData(byte[] p, int size, long timeUs, int flags) {
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

    public boolean hasFormat() {
        return (mFormat != null);
    }

    public long bufferSizeMs() {
        return (mLargestTimestampUs - mSmallestTimestampUs) / 1000;
    }

    public long getBufferedPositionUs() {
        return mLargestTimestampUs;
    }

    synchronized public boolean readFormat(MediaFormatHolder formatHolder) {
        if(!rollingBuffer.peekSample(mSampleHolder)) {
            return false;
        }

        FormatHolder h = mFormatQueue.peek();

        if(h == null) {
            return false;
        }

        if(h.timeUs > mSampleHolder.timeUs) {
            return false;
        }

        mFormat = h.format;
        formatHolder.format = h.format;

        mFormatQueue.poll();
        return true;
    }

    synchronized public boolean readSample(SampleHolder sampleHolder) {
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
