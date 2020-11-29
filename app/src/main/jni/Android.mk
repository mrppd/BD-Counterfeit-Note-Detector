LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#opencv
OPENCVROOT:= C:/Users/Pronaya/Downloads/OpenCV-android-sdk
#C:/Users/Pronaya\Downloads/OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk


LOCAL_MODULE := registrationAndPrediction
LOCAL_LDLIBS += -llog
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := prediction_f1.cpp prediction_f2.cpp prediction_f3.cpp prediction_f4.cpp register_image_front.cpp register_image_back.cpp



include $(BUILD_SHARED_LIBRARY)
