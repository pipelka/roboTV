#ifndef AC3_DECODER_H
#define AC3_DECODER_H

#include <stdint.h>
#include <stdlib.h>

extern "C" {
#include "a52.h"
}

struct AC3SyncInfo {
	int frameSizeInBytes;
	int flags;
	int sampleRate;
	int bitRate;
};

class AC3Decoder {
public:

	enum {
		LayoutStereo = A52_STEREO,
		LayoutDolby = A52_DOLBY,
		Layout50 = A52_3F2R,
		Layout51 = A52_3F2R | A52_LFE
	} ChannelLayout;

	AC3Decoder(int flags = LayoutDolby);

	virtual ~AC3Decoder();

	AC3SyncInfo getSyncInfo(char* BYTE, int offset, int length);

	bool decode(char* BYTE, int offset, int length);

	int getOutput(char* BYTE, int offset, int length);

	int getChannels() {
		return mChannels;
	}

	int getSampleRate() {
		return mSampleRate;
	}

	int getFlags() {
		return mFrameFlags;
	}

	int getOutputBufferSize() {
		return mOutputBufferLength;
	}

private:

	int mFlags;

	int mFrameFlags;

	int mChannels;

	int mSampleRate;
	
	a52_state_t* mState;

	sample_t* mSampleBuffer;

	int mOutputBufferLength;
};

#endif // AC3_DECODER_H
