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
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.ParsableByteArray;

import android.util.Log;

import org.xvdr.robotv.tv.StreamBundle;

/**
 * Processes a XVDR H264 byte stream
 */
final class H264Reader extends ElementaryStreamReader {

	private static final String TAG = "H264Reader";

    private boolean hasOutputFormat;

	// Mediaformat
	private int frameWidth;
	private int frameHeight;
	private double pixelAspectRatio;

	public H264Reader(TrackOutput output, StreamBundle.Stream stream) {
        super(output);
		frameWidth = stream.width;
		frameHeight = stream.height;

		// XVDR sends the picture aspect ratio
		// we have to convert it to the pixel aspect ratio
        double value = (stream.aspect * frameHeight) / (double)frameWidth;
        pixelAspectRatio = (double)Math.round(value * 1000) / 1000;

	}

	@Override
	public void seek() {
	}

	@Override
    public void consume(ParsableByteArray data, long pesTimeUs, boolean isKeyframe, long durationUs) {
        if(!hasOutputFormat && isKeyframe) {
            output.format(MediaFormat.createVideoFormat(
                    MimeTypes.VIDEO_H264,
                    MediaFormat.NO_VALUE,
                    durationUs,
                    frameWidth,
                    frameHeight,
                    (float) pixelAspectRatio,
                    null/*decoderInitData*/));

            hasOutputFormat = true;
            Log.i(TAG, "MediaFormat: " + frameWidth + "x" + frameHeight + " aspect: " + pixelAspectRatio);
        }

        while(data.bytesLeft() > 0) {
            output.sampleData(data, data.bytesLeft());
        }

        int flags = isKeyframe ? C.SAMPLE_FLAG_SYNC : 0;
        output.sampleMetadata(pesTimeUs, flags, data.limit(), 0, null);
    }

	@Override
	public void packetFinished() {
	}

}
