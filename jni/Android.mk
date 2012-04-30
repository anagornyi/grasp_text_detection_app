LOCAL_PATH := $(call my-dir)

TESSERACT_PATH := $(call my-dir)/../external/tesseract-3.01
LEPTONICA_PATH := $(call my-dir)/../external/leptonica-1.68
LIBJPEG_PATH := $(call my-dir)/../external/libjpeg

include $(call all-subdir-makefiles)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES := off
include $(OPENCV_PACKAGE_DIR)/share/OpenCV/OpenCV.mk

LOCAL_PATH := jni

LOCAL_MODULE    := run_detection
LOCAL_SRC_FILES := text_detect.cpp android.cpp
LOCAL_LDLIBS    += -landroid -llog -ldl

include $(BUILD_SHARED_LIBRARY)





