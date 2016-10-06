#include "mpadecoder.h"
#include <android/log.h>
#include <string.h>

#define LOG_TAG "MpegAudioDecoder"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

inline int scale(mad_fixed_t sample) {
    sample += (1L << (MAD_F_FRACBITS - 16));

    if (sample >= MAD_F_ONE)
        sample = MAD_F_ONE - 1;
    else if (sample < -MAD_F_ONE)
        sample = -MAD_F_ONE;

    return sample >> (MAD_F_FRACBITS + 1 - 16);
}

MpegAudioDecoder::MpegAudioDecoder() : Decoder(0) {
    mad_stream_init(&m_stream);
    mad_frame_init(&m_frame);
    mad_synth_init(&m_synth);
    mad_header_init(&m_header);
}

MpegAudioDecoder::~MpegAudioDecoder() {
    mad_synth_finish(&m_synth);
    mad_frame_finish(&m_frame);
    mad_stream_finish(&m_stream);
}

int MpegAudioDecoder::decode(uint8_t* src_buffer, int src_length, uint8_t* dst_buffer, int dst_length) {

    if(dst_length < 1152 * 4) {
        ALOG("output buffer too small !");
        return 0;
    }

    // make libmad happy - it needs at least 2 samples
    memcpy(src_buffer + src_length, src_buffer, src_length);
    src_length = src_length * 2;

    // fill stream buffer
    mad_stream_buffer(&m_stream, (const unsigned char*)src_buffer, src_length);

    if(mad_header_decode(&m_header, &m_stream) == -1) {
        ALOG("unable to decode header (error: %s)", mad_stream_errorstr(&m_stream));
        return 0;
    }

    if(mad_frame_decode(&m_frame, &m_stream) == -1) {
        ALOG("unable to decode frame (error: %s)", mad_stream_errorstr(&m_stream));
        return 0;
    }

    mad_synth_frame(&m_synth, &m_frame);
    struct mad_pcm* pcm = &m_synth.pcm;

    // get stream properties
    m_sampleRate = pcm->samplerate;
    m_channels = 2;

    int nsamples = pcm->length;

    const mad_fixed_t* left_ch = pcm->samples[0];
    const mad_fixed_t* right_ch = pcm->samples[1];

    if (pcm->channels == 1) {
        right_ch = pcm->samples[0];
    }

    signed short* buffer = (signed short*)dst_buffer;

    while (nsamples--) {
        *buffer++ = scale(*left_ch++);
        *buffer++ = scale(*right_ch++);
    }

    return 1152 * 4;
}
