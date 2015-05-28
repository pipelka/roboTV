#include "ac3decoder.h"
#include <android/log.h>

#define LOG_TAG "AC3Decoder"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

extern "C" {
#include "mm_accel.h"
}

AC3Decoder::AC3Decoder(int flags) : mFlags(flags) {
	 mState = a52_init(MM_ACCEL_DJBFFT);
	 mSampleBuffer = a52_samples(mState);
	 mOutputBufferLength = 0;
}

AC3Decoder::~AC3Decoder() {
	a52_free(mState);
}

AC3SyncInfo AC3Decoder::getSyncInfo(char* BYTE, int offset, int length) {
	AC3SyncInfo info;

	if(length < 7) {
		info.frameSizeInBytes = 0;
		return info;
	}

	info.flags = mFlags;
	info.frameSizeInBytes = a52_syncinfo((uint8_t*)BYTE, &info.flags, &info.sampleRate, &info.bitRate);

	return info;
}

bool AC3Decoder::decode(char* BYTE, int offset, int length) {
	// always sample to signed int range -16383 - 16383
	sample_t level = 16383;
	sample_t bias = 0;
	mFrameFlags = mFlags | A52_ADJUST_LEVEL;

	if(length < 7) {
		ALOG("decode: frame too small");
		return false;
	}

	// check frame
	AC3SyncInfo info;
	int frameLength = a52_syncinfo((uint8_t*)BYTE, &info.flags, &mSampleRate, &info.bitRate);

	if(frameLength == 0) {
		ALOG("a52_syncinfo: invalid frame !");
		return false;
	}

	if(length != frameLength) {
		ALOG("a52_syncinfo: framelength doesn't match (%i / %i)", length, frameLength);
		return false;
	}

	// feed frame data

	if(a52_frame(mState, (uint8_t*)BYTE, &mFrameFlags, &level, bias) != 0) {
		return false;
	}

	// check channel count

	mChannels = 0;


	if(mFrameFlags & A52_STEREO || mFrameFlags & A52_DOLBY) {
		mChannels = 2;
	}
	else if(mFrameFlags & A52_3F2R) {
		mChannels = 5;
	}

	if(mFrameFlags & A52_LFE) {
		mChannels++;
	}

	// output buffer length
	mOutputBufferLength = sizeof(int16_t) * mChannels * 256 * 6;
	return true;
}

int AC3Decoder::getOutput(char* BYTE, int offset, int length) {
	if(length < mOutputBufferLength) {
		ALOG("output buffer too small !");
		return 0;
	}

	int16_t* buffer = (int16_t*)BYTE;

	// process all 6 blocks
	int x = 0;
	int blockOffset = 0;

	for(int i = 0; i < 6; i++) {

		blockOffset = x++ * 256 * mChannels;

		// get block
		if(a52_block(mState) != 0) {
			continue;
		}

		// copy block data for each channel
		for(int c = 0; c < mChannels; c++) {

			int channelOffset = c * 256;

			// copy data block
			for(int s = 0; s < 256; s++) {
				buffer[s + blockOffset + channelOffset] = (int16_t)mSampleBuffer[s + channelOffset];
			}
		}
	}

	return x * 256 * mChannels;
}
