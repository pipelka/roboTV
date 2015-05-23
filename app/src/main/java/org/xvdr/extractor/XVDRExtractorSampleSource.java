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
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackInfo;
import com.google.android.exoplayer.drm.DrmInitData;
import com.google.android.exoplayer.extractor.DefaultTrackOutput;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorOutput;
import com.google.android.exoplayer.extractor.SeekMap;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.util.Assertions;

import android.util.SparseArray;

import java.io.IOException;

/**
 * A {@link XVDRExtractorSampleSource} that extracts sample data from a {@link XVDRExtractor}
 */
public class XVDRExtractorSampleSource implements SampleSource, ExtractorOutput {

	private static final int BUFFER_FRAGMENT_LENGTH = 512 * 1024;
	private static final int NO_RESET_PENDING = -1;

	private final DefaultAllocator allocator;
	private final SparseArray<DefaultTrackOutput> sampleQueues;
	private final boolean frameAccurateSeeking;

	private volatile boolean tracksBuilt;
	private volatile SeekMap seekMap;
	private volatile DrmInitData drmInitData;

	private boolean prepared;
	private int enabledTrackCount;
	private TrackInfo[] trackInfos;
	private long maxTrackDurationUs;
	private boolean[] pendingMediaFormat;
	private boolean[] pendingDiscontinuities;
	private boolean[] trackEnabledStates;

	private int remainingReleaseCount;
	private long downstreamPositionUs;
	private long lastSeekPositionUs;
	private long pendingResetPositionUs;

	/**
	 * @param extractor An {@link Extractor} to extract the media stream.
	 * @param downstreamRendererCount Number of track renderers dependent on this sample source.
	 */
	public XVDRExtractorSampleSource(XVDRExtractor extractor, int downstreamRendererCount) {
		remainingReleaseCount = downstreamRendererCount;
		sampleQueues = new SparseArray<>();
		allocator = new DefaultAllocator(BUFFER_FRAGMENT_LENGTH);
		pendingResetPositionUs = NO_RESET_PENDING;
		frameAccurateSeeking = true;
		extractor.init(this);
	}

	@Override
	public boolean prepare(long positionUs) throws IOException {
		if(prepared) {
			return true;
		}

		continueBufferingInternal();

		// TODO: Support non-seekable content? Or at least avoid getting stuck here if a seekMap doesn't
		// arrive (we may end up filling the sample buffers whilst we're still not prepared, and then
		// getting stuck).
		if(seekMap != null && tracksBuilt && haveFormatsForAllTracks()) {
			int trackCount = sampleQueues.size();
			trackEnabledStates = new boolean[trackCount];
			pendingDiscontinuities = new boolean[trackCount];
			pendingMediaFormat = new boolean[trackCount];
			trackInfos = new TrackInfo[trackCount];
			maxTrackDurationUs = C.UNKNOWN_TIME_US;

			for(int i = 0; i < trackCount; i++) {
				MediaFormat format = sampleQueues.valueAt(i).getFormat();
				trackInfos[i] = new TrackInfo(format.mimeType, format.durationUs);

				if(format.durationUs != C.UNKNOWN_TIME_US && format.durationUs > maxTrackDurationUs) {
					maxTrackDurationUs = format.durationUs;
				}
			}

			prepared = true;
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int getTrackCount() {
		return sampleQueues.size();
	}

	@Override
	public TrackInfo getTrackInfo(int track) {
		Assertions.checkState(prepared);
		return trackInfos[track];
	}

	@Override
	public void enable(int track, long positionUs) {
		Assertions.checkState(prepared);
		Assertions.checkState(!trackEnabledStates[track]);
		enabledTrackCount++;
		trackEnabledStates[track] = true;
		pendingMediaFormat[track] = true;

		if(enabledTrackCount == 1) {
			seekToUs(positionUs);
		}
	}

	@Override
	public void disable(int track) {
		Assertions.checkState(prepared);
		Assertions.checkState(trackEnabledStates[track]);
		enabledTrackCount--;
		trackEnabledStates[track] = false;
		pendingDiscontinuities[track] = false;

		if(enabledTrackCount == 0) {
			clearState();
			allocator.trim(0);
		}
	}

	@Override
	public boolean continueBuffering(long playbackPositionUs) throws IOException {
		Assertions.checkState(prepared);
		Assertions.checkState(enabledTrackCount > 0);
		downstreamPositionUs = playbackPositionUs;
		discardSamplesForDisabledTracks(downstreamPositionUs);
		return continueBufferingInternal();
	}

	@Override
	public int readData(int track, long playbackPositionUs, MediaFormatHolder formatHolder,
	                    SampleHolder sampleHolder, boolean onlyReadDiscontinuity) throws IOException {
		downstreamPositionUs = playbackPositionUs;

		if(pendingDiscontinuities[track]) {
			pendingDiscontinuities[track] = false;
			return DISCONTINUITY_READ;
		}

		if(onlyReadDiscontinuity || isPendingReset()) {
			return NOTHING_READ;
		}

		DefaultTrackOutput sampleQueue = sampleQueues.valueAt(track);

		if(pendingMediaFormat[track]) {
			formatHolder.format = sampleQueue.getFormat();
			formatHolder.drmInitData = drmInitData;
			pendingMediaFormat[track] = false;
			return FORMAT_READ;
		}

		if(sampleQueue.getSample(sampleHolder)) {
			boolean decodeOnly = frameAccurateSeeking && sampleHolder.timeUs < lastSeekPositionUs;
			sampleHolder.flags |= decodeOnly ? C.SAMPLE_FLAG_DECODE_ONLY : 0;
			return SAMPLE_READ;
		}

		return NOTHING_READ;
	}

	@Override
	public void seekToUs(long positionUs) {
		Assertions.checkState(prepared);
		Assertions.checkState(enabledTrackCount > 0);

		if(!seekMap.isSeekable()) {
			// Treat all seeks into non-seekable media as seeks to the start.
			positionUs = 0;
		}

		lastSeekPositionUs = positionUs;

		if((isPendingReset() ? pendingResetPositionUs : downstreamPositionUs) == positionUs) {
			return;
		}

		downstreamPositionUs = positionUs;

		// If we're not pending a reset, see if we can seek within the sample queues.
		boolean seekInsideBuffer = !isPendingReset();

		for(int i = 0; seekInsideBuffer && i < sampleQueues.size(); i++) {
			seekInsideBuffer &= sampleQueues.valueAt(i).skipToKeyframeBefore(positionUs);
		}

		// If we failed to seek within the sample queues, we need to restart.
		if(!seekInsideBuffer) {
			restartFrom(positionUs);
		}

		// Either way, we need to send discontinuities to the downstream components.
		for(int i = 0; i < pendingDiscontinuities.length; i++) {
			pendingDiscontinuities[i] = true;
		}
	}

	@Override
	public long getBufferedPositionUs() {
		if(isPendingReset()) {
			return pendingResetPositionUs;
		}
		else {
			long largestParsedTimestampUs = Long.MIN_VALUE;

			for(int i = 0; i < sampleQueues.size(); i++) {
				largestParsedTimestampUs = Math.max(largestParsedTimestampUs,
				                                    sampleQueues.valueAt(i).getLargestParsedTimestampUs());
			}

			return largestParsedTimestampUs == Long.MIN_VALUE ? downstreamPositionUs
			       : largestParsedTimestampUs;
		}
	}

	@Override
	public void release() {
		Assertions.checkState(remainingReleaseCount > 0);
		--remainingReleaseCount;
	}

	// ExtractorOutput implementation.

	@Override
	public TrackOutput track(int id) {
		DefaultTrackOutput sampleQueue = sampleQueues.get(id);

		if(sampleQueue == null) {
			sampleQueue = new DefaultTrackOutput(allocator);
			sampleQueues.put(id, sampleQueue);
		}

		return sampleQueue;
	}

	@Override
	public void endTracks() {
		tracksBuilt = true;
	}

	@Override
	public void seekMap(SeekMap seekMap) {
		this.seekMap = seekMap;
	}

	@Override
	public void drmInitData(DrmInitData drmInitData) {
		this.drmInitData = drmInitData;
	}

	// Internal stuff.

	private boolean continueBufferingInternal() throws IOException {
		if(isPendingReset()) {
			return false;
		}

		return prepared && haveSampleForOneEnabledTrack();
	}

	private void restartFrom(long positionUs) {
		pendingResetPositionUs = positionUs;
		clearState();
	}

	private boolean haveFormatsForAllTracks() {
		for(int i = 0; i < sampleQueues.size(); i++) {
			if(!sampleQueues.valueAt(i).hasFormat()) {
				return false;
			}
		}

		return true;
	}

	private boolean haveSampleForOneEnabledTrack() {
		for(int i = 0; i < trackEnabledStates.length; i++) {
			if(trackEnabledStates[i] && !sampleQueues.valueAt(i).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private void discardSamplesForDisabledTracks(long timeUs) {
		for(int i = 0; i < trackEnabledStates.length; i++) {
			if(!trackEnabledStates[i]) {
				sampleQueues.valueAt(i).discardUntil(timeUs);
			}
		}
	}

	private void clearState() {
		for(int i = 0; i < sampleQueues.size(); i++) {
			sampleQueues.valueAt(i).clear();
		}
	}

	private boolean isPendingReset() {
		return pendingResetPositionUs != NO_RESET_PENDING;
	}

	private long getRetryDelayMillis(long errorCount) {
		return Math.min((errorCount - 1) * 1000, 5000);
	}

}
