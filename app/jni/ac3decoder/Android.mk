LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := ac3decoder

LOCAL_SRC_FILES := \
	ac3decoder.cpp \
	ac3decoder_wrap.cpp

LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../a52dec-0.7.4/include

LOCAL_LDLIBS := -llog

JNI_WRAP := $(LOCAL_PATH)/ac3decoder_wrap.cpp
JNI_WRAP_H := $(LOCAL_PATH)/ac3decoder_wrap.h
JNI_I := $(LOCAL_PATH)/ac3decoder.i
JNI_INCLUDE := $(LOCAL_PATH)
JNI_PACKAGE := org.xvdr.ac3
JNI_DEPS := $(JNI_INCLUDE) $(LOCAL_PATH)/ac3decoder.h $(LOCAL_PATH)/ac3decoder.i

OUTDIR := $(LOCAL_PATH)/../../src/main/java/org/xvdr/ac3

$(JNI_WRAP): $(JNI_I) $(JNI_DEPS)
	#rm -Rf $(OUTDIR)
	mkdir -p $(OUTDIR)
	swig -v -c++ -java -package $(JNI_PACKAGE) -I$(JNI_INCLUDE) -outdir $(OUTDIR) -o $(JNI_WRAP) $(JNI_I)

LOCAL_SHARED_LIBRARIES = a52

include $(BUILD_SHARED_LIBRARY)
