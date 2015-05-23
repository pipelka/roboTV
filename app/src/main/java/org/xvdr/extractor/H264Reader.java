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
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.H264Util;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import android.util.Log;

import org.xvdr.tv.StreamBundle;

import java.util.Arrays;

/**
 * Processes a XVDR H264 byte stream
 */
/* package */ class H264Reader extends ElementaryStreamReader {

	private static final String TAG = "H264Reader";

	private static final int NAL_UNIT_TYPE_IDR = 5;
	private static final int NAL_UNIT_TYPE_SEI = 6;
	private static final int NAL_UNIT_TYPE_SPS = 7;
	private static final int NAL_UNIT_TYPE_PPS = 8;
	private static final int NAL_UNIT_TYPE_AUD = 9;
	private static final int EXTENDED_SAR = 0xFF;

	// State that should not be reset on seek.
	private boolean hasOutputFormat;

	// State that should be reset on seek.
	private final SeiReader seiReader;
	private final boolean[] prefixFlags;
	private final NalUnitTargetBuffer sei;
	private boolean writingSample;
	private long totalBytesWritten;

	// Per sample state that gets reset at the start of each sample.
	private long samplePosition;
	private long sampleTimeUs;

	// Scratch variables to avoid allocations.
	private final ParsableByteArray seiWrapper;
	private int[] scratchEscapePositions;

	// Mediaformat
	private int frameWidth;
	private int frameHeight;
	private double pixelAspectRatio;

	public H264Reader(TrackOutput output, SeiReader seiReader, StreamBundle.Stream stream) {
		this(output, seiReader);
		frameWidth = stream.width;
		frameHeight = stream.height;

		// XVDR sends the picture aspect ratio
		// we have to convert it to the pixel aspect ratio
		pixelAspectRatio = (stream.aspect * frameHeight) / (double)frameWidth;
	}

	protected H264Reader(TrackOutput output, SeiReader seiReader) {
		super(output);
		this.seiReader = seiReader;
		prefixFlags = new boolean[3];
		sei = new NalUnitTargetBuffer(NAL_UNIT_TYPE_SEI, 1024);
		seiWrapper = new ParsableByteArray();
		scratchEscapePositions = new int[10];
	}

	@Override
	public void seek() {
		seiReader.seek();
		H264Util.clearPrefixFlags(prefixFlags);
		sei.reset();
		writingSample = false;
		totalBytesWritten = 0;
	}

	@Override
	public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe) {
		if(!hasOutputFormat) {
			output.format(MediaFormat.createVideoFormat(
			                  MimeTypes.VIDEO_H264,
			                  MediaFormat.NO_VALUE,
			                  C.UNKNOWN_TIME_US,
			                  frameWidth,
			                  frameHeight,
			                  (float)pixelAspectRatio,
			                  null));

			hasOutputFormat = true;
			Log.i(TAG, "MediaFormat: " + frameWidth + "x" + frameHeight + " aspect: " + pixelAspectRatio);
		}

		while(data.bytesLeft() > 0) {
			int offset = data.getPosition();
			int limit = data.limit();
			byte[] dataArray = data.data;

			// Append the data to the buffer.
			totalBytesWritten += data.bytesLeft();
			output.sampleData(data, data.bytesLeft());

			// Scan the appended data, processing NAL units as they are encountered
			while(offset < limit) {
				int nextNalUnitOffset = H264Util.findNalUnit(dataArray, offset, limit, prefixFlags);

				if(nextNalUnitOffset < limit) {
					// We've seen the start of a NAL unit.

					// This is the length to the start of the unit. It may be negative if the NAL unit
					// actually started in previously consumed data.
					int lengthToNalUnit = nextNalUnitOffset - offset;

					if(lengthToNalUnit > 0) {
						feedNalUnitTargetBuffersData(dataArray, offset, nextNalUnitOffset);
					}

					int nalUnitType = H264Util.getNalUnitType(dataArray, nextNalUnitOffset);
					//Log.i(TAG, "found NAL: " + nalUnitType + " @" + nextNalUnitOffset);
					int bytesWrittenPastNalUnit = limit - nextNalUnitOffset;

					if(nalUnitType == NAL_UNIT_TYPE_AUD) {
						if(writingSample) {
							int flags = isKeyframe ? C.SAMPLE_FLAG_SYNC : 0;
							int size = (int)(totalBytesWritten - samplePosition) - bytesWrittenPastNalUnit;
							output.sampleMetadata(sampleTimeUs, flags, size, bytesWrittenPastNalUnit, null);
							writingSample = false;
						}

						writingSample = true;
						sampleTimeUs = pesTimeUs;
						samplePosition = totalBytesWritten - bytesWrittenPastNalUnit;
					}

					// If the length to the start of the unit is negative then we wrote too many bytes to the
					// NAL buffers. Discard the excess bytes when notifying that the unit has ended.
					feedNalUnitTargetEnd(pesTimeUs, lengthToNalUnit < 0 ? -lengthToNalUnit : 0);
					// Notify the start of the next NAL unit.
					feedNalUnitTargetBuffersStart(nalUnitType);
					// Continue scanning the data.
					offset = nextNalUnitOffset + 4;
				}
				else {
					feedNalUnitTargetBuffersData(dataArray, offset, limit);
					offset = limit;
				}
			}
		}
	}

	@Override
	public void packetFinished() {
		// Do nothing.
	}

	private void feedNalUnitTargetBuffersStart(int nalUnitType) {
		sei.startNalUnit(nalUnitType);
	}

	private void feedNalUnitTargetBuffersData(byte[] dataArray, int offset, int limit) {
		sei.appendToNalUnit(dataArray, offset, limit);
	}

	private void feedNalUnitTargetEnd(long pesTimeUs, int discardPadding) {
		if(sei.endNalUnit(discardPadding)) {
			int unescapedLength = unescapeStream(sei.nalData, sei.nalLength);
			seiWrapper.reset(sei.nalData, unescapedLength);
			seiReader.consume(seiWrapper, pesTimeUs, false);
		}
	}

	/**
	 * Unescapes {@code data} up to the specified limit, replacing occurrences of [0, 0, 3] with
	 * [0, 0]. The unescaped data is returned in-place, with the return value indicating its length.
	 * <p>
	 * See ISO/IEC 14496-10:2005(E) page 36 for more information.
	 *
	 * @param data The data to unescape.
	 * @param limit The limit (exclusive) of the data to unescape.
	 * @return The length of the unescaped data.
	 */
	private int unescapeStream(byte[] data, int limit) {
		int position = 0;
		int scratchEscapeCount = 0;

		while(position < limit) {
			position = findNextUnescapeIndex(data, position, limit);

			if(position < limit) {
				if(scratchEscapePositions.length <= scratchEscapeCount) {
					// Grow scratchEscapePositions to hold a larger number of positions.
					scratchEscapePositions = Arrays.copyOf(scratchEscapePositions,
					                                       scratchEscapePositions.length * 2);
				}

				scratchEscapePositions[scratchEscapeCount++] = position;
				position += 3;
			}
		}

		int unescapedLength = limit - scratchEscapeCount;
		int escapedPosition = 0; // The position being read from.
		int unescapedPosition = 0; // The position being written to.

		for(int i = 0; i < scratchEscapeCount; i++) {
			int nextEscapePosition = scratchEscapePositions[i];
			int copyLength = nextEscapePosition - escapedPosition;
			System.arraycopy(data, escapedPosition, data, unescapedPosition, copyLength);
			escapedPosition += copyLength + 3;
			unescapedPosition += copyLength + 2;
		}

		int remainingLength = unescapedLength - unescapedPosition;
		System.arraycopy(data, escapedPosition, data, unescapedPosition, remainingLength);
		return unescapedLength;
	}

	private int findNextUnescapeIndex(byte[] bytes, int offset, int limit) {
		for(int i = offset; i < limit - 2; i++) {
			if(bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x03) {
				return i;
			}
		}

		return limit;
	}

	/**
	 * A buffer that fills itself with data corresponding to a specific NAL unit, as it is
	 * encountered in the stream.
	 */
	private static final class NalUnitTargetBuffer {

		private final int targetType;

		private boolean isFilling;

		public byte[] nalData;
		public int nalLength;

		public NalUnitTargetBuffer(int targetType, int initialCapacity) {
			this.targetType = targetType;
			// Initialize data, writing the known NAL prefix into the first four bytes.
			nalData = new byte[4 + initialCapacity];
			nalData[2] = 1;
			nalData[3] = (byte) targetType;
		}

		/**
		 * Resets the buffer, clearing any data that it holds.
		 */
		public void reset() {
			isFilling = false;
		}

		/**
		 * True if the buffer currently holds a complete NAL unit of the target type.
		 */
		/*public boolean isCompleted() {
		  return isCompleted;
		}*/

		/**
		 * Invoked to indicate that a NAL unit has started.
		 *
		 * @param type The type of the NAL unit.
		 */
		public void startNalUnit(int type) {
			Assertions.checkState(!isFilling);
			isFilling = type == targetType;

			if(isFilling) {
				// Length is initially the length of the NAL prefix.
				nalLength = 4;
			}
		}

		/**
		 * Invoked to pass stream data. The data passed should not include 4 byte NAL unit prefixes.
		 *
		 * @param data Holds the data being passed.
		 * @param offset The offset of the data in {@code data}.
		 * @param limit The limit (exclusive) of the data in {@code data}.
		 */
		public void appendToNalUnit(byte[] data, int offset, int limit) {
			if(!isFilling) {
				return;
			}

			int readLength = limit - offset;

			if(nalData.length < nalLength + readLength) {
				nalData = Arrays.copyOf(nalData, (nalLength + readLength) * 2);
			}

			System.arraycopy(data, offset, nalData, nalLength, readLength);
			nalLength += readLength;
		}

		/**
		 * Invoked to indicate that a NAL unit has ended.
		 *
		 * @param discardPadding The number of excess bytes that were passed to
		 *     {@link #appendToNalUnit(byte[], int, int)}, which should be discarded.
		 * @return True if the ended NAL unit is of the target type. False otherwise.
		 */
		public boolean endNalUnit(int discardPadding) {
			if(!isFilling) {
				return false;
			}

			nalLength -= discardPadding;
			isFilling = false;
			return true;
		}

	}

}
