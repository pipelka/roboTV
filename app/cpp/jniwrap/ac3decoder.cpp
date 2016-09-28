#include <stdlib.h>
#include "ac3decoder.h"
#include <android/log.h>

#define LOG_TAG "AC3Decoder"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

Ac3Decoder::Ac3Decoder(int flags) : Decoder(flags) {
	mState = a52_init(0);
}

Ac3Decoder::~Ac3Decoder() {
	a52_free(mState);
}


int Ac3Decoder::decode(uint8_t* srcBuffer, int srcLength, uint8_t* dstBuffer, int dstLength) {
    sample_t level = 32767;
	sample_t bias = 0;
	int flags;

	if(srcLength < 7) {
		ALOG("decode: frame too small");
		return false;
	}

	int frameLength = a52_syncinfo(srcBuffer, &flags, &mSampleRate, &mBitRate);

	if(frameLength <= 0) {
		ALOG("a52_syncinfo: invalid frame !");
		return 0;
	}

	if(srcLength != frameLength) {
		ALOG("a52_syncinfo: framelength doesn't match (%i / %i)", srcLength, frameLength);
		return 0;
	}

	// feed frame data

	flags = mFlags | A52_ADJUST_LEVEL;

	if(a52_frame(mState, srcBuffer, &flags, &level, bias) != 0) {
		ALOG("a52_frame: failed to feed frame !");
		return 0;
	}

	// check channel count

	mChannels = 0;
    int channelFlags = flags & A52_CHANNEL_MASK;

	if(channelFlags == A52_STEREO || channelFlags == A52_DOLBY) {
		mChannels = 2;
	}
	else if(channelFlags & A52_3F2R) {
		mChannels = 5;
		if(flags & A52_LFE) {
			mChannels++;
		}
	}

	// output buffer length
	int output_length = sizeof(int16_t) * mChannels * 256 * 6;

    if(output_length > dstLength) {
		ALOG("Ac3Decoder: destination buffer too small (%i bytes) needed: %i bytes", dstLength, output_length);
        return 0;
    }

	// process all 6 blocks
    int16_t* buffer = (int16_t*)dstBuffer;
    int p0 = 0; // destination frame pointer

	for(int i = 0; i < 6; i++) {

		// get block
		if(a52_block(mState) != 0) {
			ALOG("failed to decode block %i", i);
			return 0;
		}

		sample_t* sample = a52_samples(mState);

		// copy block data for each channel
		for(int c = 0; c < mChannels; c++) {

			int channelOffset = c * 256;
			p0 = c + (i * 256 * mChannels);

			// copy data block (interleave samples into destination frame)
			for(int s = 0; s < 256; s++) {
				if(p0 >= dstLength) {
					ALOG("*** output buffer overrun ***");
					break;
				}
                buffer[p0] = (int16_t)sample[channelOffset++];
				p0 += mChannels;
			}
		}
	}

	return output_length;
}
