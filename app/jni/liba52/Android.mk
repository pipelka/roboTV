LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := a52

LOCAL_SRC_FILES := \
	../a52dec-0.7.4/liba52/bitstream.c \
	../a52dec-0.7.4/liba52/imdct.c \
	../a52dec-0.7.4/liba52/bit_allocate.c \
	../a52dec-0.7.4/liba52/parse.c \
	../a52dec-0.7.4/liba52/downmix.c

LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../a52dec-0.7.4/include \
	-I$(LOCAL_PATH)/a52config \
	-Wno-attributes

LOCAL_LDLIBS := \
	-lm

include $(BUILD_SHARED_LIBRARY)
