package org.xvdr.extractor;

import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

abstract class StreamReader {

	protected final DefaultTrackOutput output;
    public final StreamBundle.Stream stream;

	protected StreamReader(DefaultTrackOutput output, StreamBundle.Stream stream) {
		this.output = output;
        this.stream = stream;
	}

	public abstract void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe);

}
