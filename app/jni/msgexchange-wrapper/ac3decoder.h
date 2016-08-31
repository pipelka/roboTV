#ifndef AC3_DECODER_H
#define AC3_DECODER_H

#include "decoder.h"

extern "C" {
#include "a52.h"
}

class Ac3Decoder : public Decoder {
public:

	enum {
		LayoutStereo = A52_STEREO,
		LayoutDolby = A52_DOLBY,
		Layout50 = A52_3F2R,
		Layout51 = A52_3F2R | A52_LFE
	} ChannelLayout;

	Ac3Decoder(int flags);

	virtual ~Ac3Decoder();

	int decode(MsgPacket* p, int src_length, char* BYTE, int offset, int dst_length);

	int getChannels() {
		return mChannels;
	}

    int getSampleRate() {
        return mSampleRate;
    }

    int getBitRate() {
        return mBitRate;
    }

private:

	int mChannels;

    int mSampleRate;

    int mBitRate;

	a52_state_t* mState;
};

#endif // AC3_DECODER_H
