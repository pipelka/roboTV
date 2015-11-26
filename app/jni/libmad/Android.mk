LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := mad

LIBMADPATH = ../external/libmad-0.15.1b

LOCAL_SRC_FILES := \
	$(LIBMADPATH)/version.c \
	$(LIBMADPATH)/fixed.c \
	$(LIBMADPATH)/bit.c \
	$(LIBMADPATH)/timer.c \
	$(LIBMADPATH)/stream.c \
	$(LIBMADPATH)/frame.c \
	$(LIBMADPATH)/synth.c \
	$(LIBMADPATH)/decoder.c \
	$(LIBMADPATH)/layer12.c \
	$(LIBMADPATH)/layer3.c \
	$(LIBMADPATH)/huffman.c \

LOCAL_CFLAGS := \
	-O3 \
	-I$(LOCAL_PATH)/$(LIBMADPATH)

ifeq ($(TARGET_ARCH),x86)
    LOCAL_CFLAGS   := -DFPM_INTEL
else ifeq ($(TARGET_ARCH),arm)
    LOCAL_CFLAGS   := -DFPM_DEFAULT
else ifeq ($(TARGET_ARCH),arm64)
    LOCAL_CFLAGS   := -DFPM_DEFAULT
else ifeq ($(TARGET_ARCH),x86_64)
    LOCAL_CFLAGS   := -DFPM_64BIT
endif

include $(BUILD_SHARED_LIBRARY)
