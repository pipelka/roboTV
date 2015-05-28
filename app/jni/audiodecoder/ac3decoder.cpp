#include <stdlib.h>
#include "ac3decoder.h"
#include <android/log.h>

#define LOG_TAG "AC3Decoder"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

extern "C" {
#include "mm_accel.h"
}

inline int16_t convert (int32_t i)
{
    if (i > 0x43c07fff)
	return 32767;
    else if (i < 0x43bf8000)
	return -32768;
    else
	return i - 0x43c00000;
}

AC3Decoder::AC3Decoder(int flags) : Decoder(flags) {
	mState = a52_init(0);
	mOutputBufferLength = 0;
	mDecodeBuffer = NULL;
}

AC3Decoder::~AC3Decoder() {
	a52_free(mState);
	free(mDecodeBuffer);
}

int AC3Decoder::decode(char* BYTE, int offset, int length) {
	sample_t level = 32767;
	sample_t bias = 0;
	int bitRate;
	int flags;

	uint8_t* inputBuffer = (uint8_t*)&BYTE[offset];

	if(length < 7) {
		ALOG("decode: frame too small");
		return false;
	}

	// check frame
	int frameLength = a52_syncinfo(inputBuffer, &flags, &mSampleRate, &bitRate);

	if(frameLength <= 0) {
		ALOG("a52_syncinfo: invalid frame !");
		return false;
	}

	if(length != frameLength) {
		ALOG("a52_syncinfo: framelength doesn't match (%i / %i)", length, frameLength);
		return false;
	}

	if(mDecodeBuffer == NULL) {
		int decodeBufferSize = sizeof(int16_t) * 256 * 6 * 12;
		mDecodeBuffer = (uint8_t*)memalign(16, decodeBufferSize);
	}

	// feed frame data

	flags = mFlags | A52_ADJUST_LEVEL;

	if(a52_frame(mState, inputBuffer, &flags, &level, bias) != 0) {
		ALOG("a52_frame: failed to feed frame !");
		return false;
	}

	// check channel count

	mChannels = 0;

	if(flags & A52_STEREO || flags & A52_DOLBY) {
		mChannels = 2;
	}
	else if(flags & A52_3F2R) {
		mChannels = 5;
		if(flags & A52_LFE) {
			mChannels++;
		}
	}


	// output buffer length
	mOutputBufferLength = sizeof(int16_t) * mChannels * 256 * 6;

	int16_t* buffer = (int16_t*)mDecodeBuffer;

	// process all 6 blocks
	int x = 0;
	int blockOffset = 0;
	int p = 0; // destination frame pointer

	for(int i = 0; i < 6; i++) {

		// get block
		if(a52_block(mState) != 0) {
			ALOG("failed to decode block %i", i);
			return 0;
		}

		float* sample = (float*)a52_samples(mState);

		// copy block data for each channel
		for(int c = 0; c < mChannels; c++) {

			int channelOffset = c * 256;
			p = c + (x * 256 * mChannels);

			// copy data block (interleave samples into destination frame)
			for(int s = 0; s < 256; s++) {
				if(p >= mOutputBufferLength) {
					ALOG("*** output buffer overrun ***");
					break;
				}
				buffer[p] = (int32_t)sample[channelOffset++];
				p += mChannels;
			}
		}

		x++;
	}

	return mOutputBufferLength;
}

bool AC3Decoder::getOutput(char* BYTE, int offset, int length) {
	if(length < mOutputBufferLength) {
		ALOG("output buffer too small !");
		return false;
	}

	memcpy(&BYTE[offset], mDecodeBuffer, mOutputBufferLength);
	return true;
}
