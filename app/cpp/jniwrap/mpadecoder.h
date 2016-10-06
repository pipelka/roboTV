#ifndef MPEGAUDIO_DECODER_H
#define MPEGAUDIO_DECODER_H

#include "decoder.h"

extern "C" {
#include "mad.h"
}

class MpegAudioDecoder : public Decoder {
public:

    MpegAudioDecoder();

	virtual ~MpegAudioDecoder();

	int decode(uint8_t* src_buffer, int src_length, uint8_t* dst_buffer, int dst_length);

	int getChannels() {
		return m_channels;
	}

    int getSampleRate() {
		return m_sampleRate;
	}

private:

	int m_channels;

	int m_sampleRate;

    struct mad_stream m_stream;

    struct mad_frame m_frame;

    struct mad_synth m_synth;

    struct mad_header m_header;
};

#endif // MPEGAUDIO_DECODER_H
