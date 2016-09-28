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
    init();
    mInputLength = 0;
}

MpegAudioDecoder::~MpegAudioDecoder() {
    finish();
}

void MpegAudioDecoder::init() {
    mad_stream_init(&mStream);
    mad_frame_init(&mFrame);
    mad_synth_init(&mSynth);
    mad_header_init(&mHeader);
}

void MpegAudioDecoder::finish() {
    mad_synth_finish(&mSynth);
    mad_frame_finish(&mFrame);
    mad_stream_finish(&mStream);
}

int MpegAudioDecoder::decode(uint8_t* buffer, int length) {

    // fill input buffer
    memcpy(&mInputBuffer[mInputLength], buffer, length);
    mInputLength += length;

    if(mInputLength < 2048) {
        return 0;
    }

    // fill stream buffer
    mad_stream_buffer(&mStream, (const unsigned char*)mInputBuffer, mInputLength);

    if(mad_header_decode(&mHeader, &mStream) == -1) {
        ALOG("unable to decode header (error: %s)", mad_stream_errorstr(&mStream));
        return 0;
    }

    if(mad_frame_decode(&mFrame, &mStream) == -1) {
        ALOG("unable to decode frame (error: %s)", mad_stream_errorstr(&mStream));
        return 0;
    }

    mad_synth_frame(&mSynth, &mFrame);
    struct mad_pcm* pcm = &mSynth.pcm;

    // get stream properties
    mSampleRate = pcm->samplerate;
    mChannels = 2;

    // convert samples
    prepareBuffer(&mHeader, &mSynth.pcm);

    // move input buffer
    memmove(mInputBuffer, &mInputBuffer[length], mInputLength - length);
    mInputLength -= length;

    return sizeof(mBuffer);
}

void MpegAudioDecoder::prepareBuffer(const struct mad_header *header, struct mad_pcm *pcm) {
    int nsamples = pcm->length;

    const mad_fixed_t* left_ch = pcm->samples[0];
    const mad_fixed_t* right_ch = pcm->samples[1];

    if (pcm->channels == 1) {
        right_ch = pcm->samples[0];
    }

    signed short* buffer = (signed short*)mBuffer;
    signed int sample;

    while (nsamples--) {
        *buffer++ = scale(*left_ch++);
        *buffer++ = scale(*right_ch++);
    }
}

bool MpegAudioDecoder::read(uint8_t* buffer, int length) {
    if(length < sizeof(mBuffer)) {
        ALOG("output buffer too small !");
        return false;
    }

    // copy buffer
    memcpy(buffer, mBuffer, sizeof(mBuffer));
    return true;
}

int MpegAudioDecoder::decode(uint8_t* src_buffer, int src_length, uint8_t* dst_buffer, int dst_length) {
    int length = decode(src_buffer, src_length);

    if(length == 0 || !read(dst_buffer, length)) {
        return 0;
    }

    return length;
}
