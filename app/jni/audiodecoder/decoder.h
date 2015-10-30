#ifndef DECODER_H
#define DECODER_H

#include <stdint.h>
#include <stdlib.h>

class Decoder {
public:

	Decoder(int flags) : mFlags(flags) {};

	virtual ~Decoder() {};

	virtual int decode(char* BYTE, int offset, int length) = 0;

	virtual bool read(char* BYTE, int offset, int length) = 0;

    virtual int getChannels() = 0;

	virtual int getSampleRate() = 0;

protected:

	int mFlags;

};

#endif // DECODER_H
