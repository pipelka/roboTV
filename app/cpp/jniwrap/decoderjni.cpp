#include <jni.h>
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

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
