LOCAL_PATH := $(call my-dir)
BASE_PATH := $(LOCAL_PATH)

include $(BASE_PATH)/msgexchange/jni/Android.mk
include $(BASE_PATH)/msgexchange-wrapper/Android.mk
include $(BASE_PATH)/liba52/Android.mk
include $(BASE_PATH)/ac3decoder/Android.mk
