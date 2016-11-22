#include <jni.h>
#include "ac3decoder.h"
#include "mpadecoder.h"

#define LOG_TAG "decodejni"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_org_xvdr_player_audio_Ac3Decoder_init(JNIEnv *env, jobject instance, jint flags) {
    return (jlong)new Ac3Decoder(flags);
}

JNIEXPORT void JNICALL
Java_org_xvdr_player_audio_Ac3Decoder_release(JNIEnv *env, jobject instance, jlong context) {
    Ac3Decoder* decoder = (Ac3Decoder*)context;
    delete decoder;
}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_Ac3Decoder_decode(JNIEnv *env, jobject instance,
                                                      jlong context,
                                                      jobject input, jint inputLength,
                                                      jobject output) {
    Ac3Decoder* decoder = (Ac3Decoder*)context;

    uint8_t *inputBuffer = (uint8_t *) env->GetDirectBufferAddress(input);
    int inputBufferSize = (int) (env->GetDirectBufferCapacity(input));

    uint8_t *outputBuffer = (uint8_t *) env->GetDirectBufferAddress(output);
    int outputBufferSize = (int) (env->GetDirectBufferCapacity(output));

    return decoder->decode(inputBuffer, inputLength, outputBuffer, outputBufferSize);
}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_Ac3Decoder_getChannelCount(JNIEnv *env, jobject instance, jlong context) {
    Ac3Decoder* decoder = (Ac3Decoder*)context;
    return decoder->getChannels();
}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_Ac3Decoder_getSampleRate(JNIEnv *env, jobject instance, jlong context) {
    Ac3Decoder* decoder = (Ac3Decoder*)context;
    return decoder->getSampleRate();
}


JNIEXPORT jlong JNICALL
Java_org_xvdr_player_audio_MpegAudioDecoder_init(JNIEnv *env, jobject instance) {
    return (jlong) new MpegAudioDecoder();
}

JNIEXPORT void JNICALL
Java_org_xvdr_player_audio_MpegAudioDecoder_release__J(JNIEnv *env, jobject instance, jlong context) {
    delete (MpegAudioDecoder*)context;
}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_MpegAudioDecoder_decode__JLjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2(
        JNIEnv *env, jobject instance, jlong context, jobject input, jint inputLength,
        jobject output) {
    MpegAudioDecoder* decoder = (MpegAudioDecoder*)context;

    uint8_t *inputBuffer = (uint8_t *) env->GetDirectBufferAddress(input);
    int inputBufferSize = (int) (env->GetDirectBufferCapacity(input));

    uint8_t *outputBuffer = (uint8_t *) env->GetDirectBufferAddress(output);
    int outputBufferSize = (int) (env->GetDirectBufferCapacity(output));

    return decoder->decode(inputBuffer, inputLength, outputBuffer, outputBufferSize);

}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_MpegAudioDecoder_getChannelCount(JNIEnv *env, jobject instance,
                                                         jlong context) {
    MpegAudioDecoder* decoder = (MpegAudioDecoder*)context;
    return decoder->getChannels();
}

JNIEXPORT jint JNICALL
Java_org_xvdr_player_audio_MpegAudioDecoder_getSampleRate(JNIEnv *env, jobject instance,
                                                       jlong context) {
    MpegAudioDecoder* decoder = (MpegAudioDecoder*)context;
    return decoder->getSampleRate();
}

JNIEXPORT jstring JNICALL
Java_org_xvdr_player_extractor_ExtractorBufferPacket_nativeString(JNIEnv *env, jobject instance,
                                                           jbyteArray buffer_, jint offset, jint remaining) {
    const char* emptyString = "";

    if(remaining <= 0) {
        return env->NewStringUTF(emptyString);
    }

    jbyte *buffer = env->GetByteArrayElements(buffer_, nullptr);

    const char* value = (char*)&buffer[offset];
    size_t length = strnlen(value, remaining);

    if(remaining == length) {
        env->ReleaseByteArrayElements(buffer_, buffer, 0);
        return env->NewStringUTF(emptyString);
    }

    jstring result = nullptr;

    try {
        result = env->NewStringUTF(value);
    }
    catch(...) {
        result = env->NewStringUTF(emptyString);
    }

    env->ReleaseByteArrayElements(buffer_, buffer, 0);
    return result;
}

#ifdef __cplusplus
}
#endif
