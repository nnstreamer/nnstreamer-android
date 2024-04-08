include $(CLEAR_VARS)

# nnstreamer_api
LOCAL_PATH := $(call my-dir)
LOCAL_MODULE := nnstreamer_api
LOCAL_SHARED_LIBRARIES := gstreamer_android
NNSTREAMER_API_OPTION := all

# GStreamer
include $(BUILD_SHARED_LIBRARY)

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/armv7
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/arm64
else ifeq ($(TARGET_ARCH_ABI),x86)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/x86
else ifeq ($(TARGET_ARCH_ABI),x86_64)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/x86_64
else
$(error Target arch ABI not supported: $(TARGET_ARCH_ABI))
endif

GSTREAMER_NDK_BUILD_PATH := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/
include $(ML_API_ROOT)/java/android/nnstreamer/src/main/jni/Android-gst-plugins.mk

GST_BLOCKED_PLUGINS      := \
        fallbackswitch livesync rsinter rstracers \
        threadshare togglerecord cdg claxon dav1d rsclosedcaption \
        ffv1 fmp4 mp4 gif hsv lewton rav1e json rspng regex textwrap textahead \
        aws hlssink3 ndi rsonvif raptorq reqwest rsrtp rsrtsp webrtchttp rswebrtc uriplaylistbin \
        rsaudiofx rsvideofx

GSTREAMER_PLUGINS        := $(filter-out $(GST_BLOCKED_PLUGINS), $(GST_REQUIRED_PLUGINS))
GSTREAMER_EXTRA_DEPS     := $(GST_REQUIRED_DEPS) glib-2.0 gio-2.0 gmodule-2.0
GSTREAMER_EXTRA_LIBS     := $(GST_REQUIRED_LIBS) -liconv

ifeq ($(NNSTREAMER_API_OPTION),all)
GSTREAMER_EXTRA_LIBS += -lcairo
endif

GSTREAMER_INCLUDE_FONTS := no
GSTREAMER_INCLUDE_CA_CERTIFICATES := no

include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk

$(call import-module, android/cpufeatures)
