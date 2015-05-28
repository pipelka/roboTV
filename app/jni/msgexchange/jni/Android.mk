LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := msgexchange
LOCAL_SRC_FILES := \
	../src/msgcondition.cpp \
	../src/msgconnection.cpp \
	../src/msghandler.cpp \
	../src/msghandlerbase.cpp \
	../src/msgpacket.cpp \
	../src/msgserver.cpp \
	../src/msgsession.cpp \
	../src/msgthread.cpp \
	../src/msgthreadqueue.cpp \
	../src/os-config.cpp

LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../include \
	-DHAVE_ZLIB=1

LOCAL_CPPFLAGS += -std=c++0x -pthread -frtti -fexceptions

LOCAL_LDLIBS := -lz

include $(BUILD_SHARED_LIBRARY)
