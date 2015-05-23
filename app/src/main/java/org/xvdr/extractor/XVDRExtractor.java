/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xvdr.extractor;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorInput;
import com.google.android.exoplayer.extractor.ExtractorOutput;
import com.google.android.exoplayer.extractor.PositionHolder;
import com.google.android.exoplayer.extractor.SeekMap;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.IOException;

import org.xvdr.msgexchange.Packet;
import org.xvdr.msgexchange.Session;
import org.xvdr.tv.ServerConnection;
import org.xvdr.tv.StreamBundle;

/**
 * Facilitates the extraction of stream data from the XVDR connection
 */
public final class XVDRExtractor implements Extractor, SeekMap, Session.Callback {

	private static final String TAG = "XVDRExtractor";

	private static final int TS_STREAM_TYPE_AAC = 0x0F;
	private static final int TS_STREAM_TYPE_ATSC_AC3 = 0x81;
	private static final int TS_STREAM_TYPE_ATSC_E_AC3 = 0x87;
	private static final int TS_STREAM_TYPE_H264 = 0x1B;
	private static final int TS_STREAM_TYPE_ID3 = 0x15;
	private static final int TS_STREAM_TYPE_EIA608 = 0x100; // 0xFF + 1

	private static final long MAX_PTS = 0x1FFFFFFFFL;

	private final long firstSampleTimestampUs;
	final SparseBooleanArray streamTypes;
	final SparseBooleanArray allowedPassthroughStreamTypes;
	final SparseArray<ElementaryStreamReader> streamReaders; // Indexed by pid

	private ExtractorOutput output;
	private long timestampOffsetUs;
	private long lastPts;

	private StreamBundle mBundle = new StreamBundle();

	public XVDRExtractor() {
		this(0, null);
	}

	public XVDRExtractor(long firstSampleTimestampUs, AudioCapabilities audioCapabilities) {
		this.firstSampleTimestampUs = firstSampleTimestampUs;
		streamTypes = new SparseBooleanArray();
		allowedPassthroughStreamTypes = getPassthroughStreamTypes(audioCapabilities);
		streamReaders = new SparseArray<>();
		lastPts = Long.MIN_VALUE;
	}

	// Extractor implementation.

	@Override
	public void init(ExtractorOutput output) {
		this.output = output;
		output.seekMap(this);
	}

	@Override
	public void seek() {
		timestampOffsetUs = 0;
		lastPts = Long.MIN_VALUE;

		for(int i = 0; i < streamReaders.size(); i++) {
			streamReaders.valueAt(i).seek();
		}
	}

	@Override
	public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
		return RESULT_CONTINUE;
	}

	// SeekMap implementation.

	@Override
	public boolean isSeekable() {
		return false;
	}

	@Override
	public long getPosition(long timeUs) {
		return 0;
	}

	// Internals.

	/**
	 * Adjusts a PTS value to the corresponding time in microseconds, accounting for PTS wraparound.
	 *
	 * @param pts The raw PTS value.
	 * @return The corresponding time in microseconds.
	 */
	/* package */ long ptsToTimeUs(long pts) {
		long timeUs = pts;
		pts = (timeUs * 90000) / C.MICROS_PER_SECOND;

		if(lastPts != Long.MIN_VALUE) {
			// The wrap count for the current PTS may be closestWrapCount or (closestWrapCount - 1),
			// and we need to snap to the one closest to lastPts.
			long closestWrapCount = (lastPts + (MAX_PTS / 2)) / MAX_PTS;
			long ptsWrapBelow = pts + (MAX_PTS * (closestWrapCount - 1));
			long ptsWrapAbove = pts + (MAX_PTS * closestWrapCount);
			pts = Math.abs(ptsWrapBelow - lastPts) < Math.abs(ptsWrapAbove - lastPts)
			      ? ptsWrapBelow : ptsWrapAbove;
		}

		// Calculate the corresponding timestamp.
		timeUs = (pts * C.MICROS_PER_SECOND) / 90000;

		// If we haven't done the initial timestamp adjustment, do it now.
		if(lastPts == Long.MIN_VALUE) {
			timestampOffsetUs = firstSampleTimestampUs - timeUs;
		}

		// Record the adjusted PTS to adjust for wraparound next time.
		lastPts = pts;
		return timeUs + timestampOffsetUs;
	}

	/**
	 * Returns a sparse boolean array of stream types that can be played back based on
	 * {@code audioCapabilities}.
	 */
	private static SparseBooleanArray getPassthroughStreamTypes(AudioCapabilities audioCapabilities) {
		SparseBooleanArray streamTypes = new SparseBooleanArray();

		if(audioCapabilities != null) {
			if(audioCapabilities.supportsEncoding(C.ENCODING_AC3)) {
				streamTypes.put(TS_STREAM_TYPE_ATSC_AC3, true);
			}

			if(audioCapabilities.supportsEncoding(C.ENCODING_E_AC3)) {
				// TODO: Uncomment when Ac3Reader supports enhanced AC-3.
				// streamTypes.put(TS_STREAM_TYPE_ATSC_E_AC3, true);
			}
		}

		return streamTypes;
	}

	void createStreams(Packet p) {
		mBundle.updateFromPacket(p);

		for(int i = 0, nsize = mBundle.size(); i < nsize; i++) {
			addStream(mBundle.valueAt(i));
		}

		output.endTracks();
	}

	void addStream(StreamBundle.Stream stream) {
		ElementaryStreamReader reader = null;

		switch(stream.getMimeType()) {
			case MimeTypes.VIDEO_H264:
				SeiReader seiReader = new SeiReader(output.track(TS_STREAM_TYPE_EIA608));
				reader = new H264Reader(output.track(TS_STREAM_TYPE_H264), seiReader, stream);
				streamTypes.put(TS_STREAM_TYPE_H264, true);
				break;

			case MimeTypes.AUDIO_AAC:
				reader = new AdtsReader(output.track(TS_STREAM_TYPE_AAC));
				streamTypes.put(TS_STREAM_TYPE_AAC, true);
				break;

			case MimeTypes.AUDIO_AC3:
				if(allowedPassthroughStreamTypes.get(TS_STREAM_TYPE_ATSC_AC3)) {
					reader = new Ac3Reader(output.track(TS_STREAM_TYPE_ATSC_AC3));
					streamTypes.put(TS_STREAM_TYPE_ATSC_AC3, true);
				}

				break;
		}

		if(reader != null) {
			Log.i(TAG, "added stream " + stream.physicalId + " (" + stream.type + ")");
			streamReaders.put(stream.physicalId, reader);
		}
	}

	// Session.Callback

	@Override
	public void onNotification(Packet notification) {
		// process STREAMCHANGE notification
		if(notification.getMsgID() == ServerConnection.XVDR_STREAM_CHANGE) {
			createStreams(notification);
			return;
		}

		// exit if it's not a MUXPKT
		if(notification.getMsgID() != ServerConnection.XVDR_STREAM_MUXPKT) {
			return;
		}

		// read pid of packet
		int pid = (int) notification.getU16();
		ElementaryStreamReader reader = streamReaders.get(pid);

		if(reader == null) {
			return;
		}

		// read packet properties

		long pts = notification.getS64();
		long dts = notification.getS64();
		int duration = (int) notification.getU32();
		int length = (int) notification.getU32();

		// read buffer
		byte[] buffer = new byte[length];
		notification.readBuffer(buffer, 0, length);

		int frameType = (int) notification.getClientID();

		reader.consume(new ParsableByteArray(buffer, length), ptsToTimeUs(pts), (frameType == 2));
	}

	@Override
	public void onDisconnect() {

	}

	@Override
	public void onReconnect() {

	}
}
