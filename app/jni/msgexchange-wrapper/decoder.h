#ifndef DECODER_H
#define DECODER_H

#include <stdint.h>
#include <stdlib.h>

#include "msgpacket.h"

class Decoder {
public:

	Decoder(int flags) : mFlags(flags) {};

	virtual ~Decoder() {};

	virtual int decode(MsgPacket* p, int src_length, char* BYTE, int offset, int dst_length) = 0;

    virtual int getChannels() = 0;

	virtual int getSampleRate() = 0;

protected:

	int mFlags;

};

#endif // DECODER_H
