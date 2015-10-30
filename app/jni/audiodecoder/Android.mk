LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := audiodecoder

LOCAL_SRC_FILES := \
	ac3decoder.cpp \
	mpadecoder.cpp \
	audiodecoder_wrap.cpp

LOCAL_CFLAGS := \
	-O3 -fomit-frame-pointer \
	-I$(LOCAL_PATH)/../external/a52dec-0.7.4/include \
	-I$(LOCAL_PATH)/../external/libmad-0.15.1b

LOCAL_LDLIBS := -llog

JNI_AUDIO_WRAP := $(LOCAL_PATH)/audiodecoder_wrap.cpp
JNI_AUDIO_WRAP_H := $(LOCAL_PATH)/audiodecoder_wrap.h
JNI_AUDIO_I := $(LOCAL_PATH)/audiodecoder.i
JNI_AUDIO_INCLUDE := $(LOCAL_PATH)
JNI_AUDIO_PACKAGE := org.xvdr.audio
JNI_AUDIO_DEPS := $(JNI_AUDIO_INCLUDE) $(LOCAL_PATH)/ac3decoder.h $(LOCAL_PATH)/audiodecoder.i

JNI_AUDIO_OUTDIR := $(LOCAL_PATH)/../../src/main/java/org/xvdr/audio

$(JNI_AUDIO_WRAP): $(JNI_AUDIO_I) $(JNI_AUDIO_DEPS)
	mkdir -p $(JNI_AUDIO_OUTDIR)
	swig -v -c++ -java -package $(JNI_AUDIO_PACKAGE) -I$(JNI_AUDIO_INCLUDE) -outdir $(JNI_AUDIO_OUTDIR) -o $(JNI_AUDIO_WRAP) $(JNI_AUDIO_I)

LOCAL_SHARED_LIBRARIES := a52 libmad

include $(BUILD_SHARED_LIBRARY)
