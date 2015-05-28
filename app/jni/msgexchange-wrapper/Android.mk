LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := msgexchange_wrapper

LOCAL_SRC_FILES := \
	msgexchange_wrap.cpp

LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../ffmpeg/jni/$(TARGET_ARCH)/include \
	-I$(LOCAL_PATH)/../msgexchange/include

LOCAL_CPPFLAGS += -std=c++0x -pthread -D__STDC_CONSTANT_MACROS -frtti -fexceptions

LOCAL_LDLIBS := \
	-llog

JNI_WRAP := $(LOCAL_PATH)/msgexchange_wrap.cpp
JNI_WRAP_H := $(LOCAL_PATH)/msgexchange_wrap.h
JNI_I := $(LOCAL_PATH)/msgexchange.i
JNI_INCLUDE := $(LOCAL_PATH)/../msgexchange/include
JNI_PACKAGE := org.xvdr.msgexchange
JNI_DEPS := $(JNI_INCLUDE) $(LOCAL_PATH)/../msgexchange/src/*.cpp $(LOCAL_PATH)/msgexchange.i

OUTDIR := $(LOCAL_PATH)/../../src/main/java/org/xvdr/msgexchange

$(JNI_WRAP): $(JNI_I) $(JNI_DEPS)
	#rm -Rf $(OUTDIR)
	mkdir -p $(OUTDIR)
	swig -v -c++ -java -package $(JNI_PACKAGE) -I$(JNI_INCLUDE) -outdir $(OUTDIR) -o $(JNI_WRAP) $(JNI_I)

LOCAL_SHARED_LIBRARIES = msgexchange avutil avformat avcodec

include $(BUILD_SHARED_LIBRARY)
