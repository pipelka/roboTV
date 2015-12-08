package org.xvdr.extractor;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

import java.util.Queue;

abstract class StreamReader {

	protected final PacketQueue output;
    public final StreamBundle.Stream stream;

	protected StreamReader(PacketQueue output, StreamBundle.Stream stream) {
		this.output = output;
        this.stream = stream;
	}

	public abstract void consume(byte[] data, long pesTimeUs, boolean isKeyframe);

}
