package org.xvdr.extractor;

import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.ParsableByteArray;

import org.xvdr.robotv.tv.StreamBundle;

abstract class StreamReader {

	protected final TrackOutput output;
    public final StreamBundle.Stream stream;

	protected StreamReader(TrackOutput output, StreamBundle.Stream stream) {
		this.output = output;
        this.stream = stream;
	}

	public abstract void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe);

}
