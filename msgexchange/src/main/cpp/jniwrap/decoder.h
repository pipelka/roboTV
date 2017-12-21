#ifndef DECODER_H
#define DECODER_H

#include <stdint.h>
#include <stdlib.h>

#include "msgpacket.h"

class Decoder {
public:

	Decoder(int flags) : mFlags(flags) {};

	inline int getFlags() {
		return mFlags;
	}

	virtual ~Decoder() {};

	virtual int decode(uint8_t* srcBuffer, int srcLength, uint8_t* dstBuffer, int dstLength) = 0;

    virtual int getChannels() = 0;

	virtual int getSampleRate() = 0;

protected:

	int mFlags;

};

#endif // DECODER_H
