#ifndef AC3_DECODER_H
#define AC3_DECODER_H

#include "decoder.h"

extern "C" {
#include "a52.h"
}

class AC3Decoder : public Decoder {
public:

	enum {
		LayoutStereo = A52_STEREO,
		LayoutDolby = A52_DOLBY,
		Layout50 = A52_3F2R,
		Layout51 = A52_3F2R | A52_LFE
	} ChannelLayout;

	AC3Decoder(int flags);

	virtual ~AC3Decoder();

	int decode(char* BYTE, int offset, int length);

	bool getOutput(char* BYTE, int offset, int length);

	int getChannels() {
		return mChannels;
	}

	int getSampleRate() {
		return mSampleRate;
	}

private:

	int mChannels;

	int mSampleRate;
	
	a52_state_t* mState;

	int mOutputBufferLength;

	uint8_t* mDecodeBuffer;
};

#endif // AC3_DECODER_H
