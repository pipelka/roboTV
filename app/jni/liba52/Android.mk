LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := a52

LOCAL_SRC_FILES := \
	../external/a52dec-0.7.4/liba52/bitstream.c \
	../external/a52dec-0.7.4/liba52/imdct.c \
	../external/a52dec-0.7.4/liba52/bit_allocate.c \
	../external/a52dec-0.7.4/liba52/parse.c \
	../external/a52dec-0.7.4/liba52/downmix.c

LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../external/a52dec-0.7.4/include \
	-I$(LOCAL_PATH)/a52config \
	-Wno-attributes

LOCAL_LDLIBS := \
	-lm

include $(BUILD_SHARED_LIBRARY)
