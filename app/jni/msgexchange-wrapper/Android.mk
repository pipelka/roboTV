LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cpp
LOCAL_MODULE    := msgexchange_wrapper

LOCAL_SRC_FILES := \
	msgexchange_wrap.cpp

LOCAL_CPPFLAGS += -std=c++0x -pthread -frtti -fexceptions
LOCAL_CFLAGS := \
	-I$(LOCAL_PATH)/../msgexchange/include

JNI_MSGEXCHANGE__WRAP := $(LOCAL_PATH)/msgexchange_wrap.cpp
JNI_MSGEXCHANGE__WRAP_H := $(LOCAL_PATH)/msgexchange_wrap.h
JNI_MSGEXCHANGE__I := $(LOCAL_PATH)/msgexchange.i
JNI_MSGEXCHANGE__INCLUDE := $(LOCAL_PATH)/../msgexchange/include
JNI_MSGEXCHANGE__PACKAGE := org.xvdr.msgexchange
JNI_MSGEXCHANGE__DEPS := $(JNI_MSGEXCHANGE__INCLUDE) $(LOCAL_PATH)/msgexchange.i

OUTDIR := $(LOCAL_PATH)/../../src/main/java/org/xvdr/msgexchange

$(JNI_MSGEXCHANGE__WRAP): $(JNI_MSGEXCHANGE__I) $(JNI_MSGEXCHANGE__DEPS)
	#rm -Rf $(OUTDIR)
	mkdir -p $(OUTDIR)
	swig -v -c++ -java -package $(JNI_MSGEXCHANGE__PACKAGE) -I$(JNI_MSGEXCHANGE__INCLUDE) -outdir $(OUTDIR) -o $(JNI_MSGEXCHANGE__WRAP) $(JNI_MSGEXCHANGE__I) || true

LOCAL_SHARED_LIBRARIES := msgexchange

include $(BUILD_SHARED_LIBRARY)
