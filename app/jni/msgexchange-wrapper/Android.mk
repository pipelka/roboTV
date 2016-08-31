LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := msgexchange_wrapper

LOCAL_SRC_FILES := \
	msgexchange_wrap.cpp \
	ac3decoder.cpp \
	mpadecoder.cpp

LOCAL_CPPFLAGS += -std=c++0x -pthread -frtti -fexceptions
LOCAL_CFLAGS := \
	-O3 -fomit-frame-pointer \
	-I$(LOCAL_PATH)/../external/a52dec-0.7.4/include \
	-I$(LOCAL_PATH)/../external/libmad-0.15.1b \
	-I$(LOCAL_PATH)/../msgexchange/include

JNI_MSGEXCHANGE__WRAP := $(LOCAL_PATH)/msgexchange_wrap.cpp
JNI_MSGEXCHANGE__WRAP_H := $(LOCAL_PATH)/msgexchange_wrap.h
JNI_MSGEXCHANGE__I := $(LOCAL_PATH)/msgexchange.i
JNI_MSGEXCHANGE__INCLUDE := $(LOCAL_PATH)/../msgexchange/include
JNI_MSGEXCHANGE__PACKAGE := org.xvdr.msgexchange
JNI_MSGEXCHANGE__DEPS := $(JNI_MSGEXCHANGE__INCLUDE) $(LOCAL_PATH)/msgexchange.i

OUTDIR := $(LOCAL_PATH)/../../src/main/java/org/xvdr/msgexchange

swig: $(JNI_MSGEXCHANGE__I) $(JNI_MSGEXCHANGE__DEPS)
	mkdir -p $(OUTDIR)
	swig -v -c++ -java -package $(JNI_MSGEXCHANGE__PACKAGE) -I$(JNI_MSGEXCHANGE__INCLUDE) -outdir $(OUTDIR) -o $(JNI_MSGEXCHANGE__WRAP) $(JNI_MSGEXCHANGE__I)

LOCAL_SHARED_LIBRARIES := msgexchange a52 libmad
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

.PHONY: swig
