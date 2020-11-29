//
// Created by Pronaya on 9/5/2016.
//

#include <jni.h>
#include "net_pronaya_ndkdemo_nativeInterface.h"

#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <vector>
#include <algorithm>
#include <stdlib.h>
#include <string>
using namespace std;
using namespace cv;


Mat find_edge_image4(Mat img_f1)
{
    double otsu_thresh_val, high_thresh_val, lower_thresh_val;
    Mat binaryMat(img_f1.size(), img_f1.type());
    //Apply thresholding
    otsu_thresh_val = threshold(img_f1, binaryMat, 0, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);

    high_thresh_val  = otsu_thresh_val;
    lower_thresh_val = otsu_thresh_val * 0.5;
    Canny( img_f1, img_f1, lower_thresh_val, high_thresh_val );
    binaryMat.release();
    return img_f1;
}

Mat get_sharpened_image4(Mat img, double sigma = 1, double threshold = 5, double amount = 1)
{
    // sharpen image using "unsharp mask" algorithm
    Mat blurred;
    GaussianBlur(img, blurred, Size(), sigma, sigma);
    Mat lowContrastMask = abs(img - blurred) < threshold;
    Mat sharpened = img*(1+amount) + blurred*(-amount);
    img.copyTo(sharpened, lowContrastMask);
    //sharpened.copyTo(img(Rect(0, 0, sharpened.cols, sharpened.rows)));
    blurred.release();
    lowContrastMask.release();
    return sharpened;
}

vector< float > hog_extraction4(Mat img )
{
    HOGDescriptor d(Size(128, 64), Size(16,16), Size(8,8), Size(8,8), 9);
    // Size(128,64), //winSize
    // Size(16,16), //blocksize
    // Size(8,8), //blockStride,
    // Size(8,8), //cellSize,
    // 9, //nbins,
    // 0, //derivAper,
    // -1, //winSigma,
    // 0, //histogramNormType,
    // 0.2, //L2HysThresh,
    // 0 //gammal correction,
    // //nlevels=64
    //);

    // void HOGDescriptor::compute(const Mat& img, vector<float>& descriptors,
    //                             Size winStride, Size padding,
    //                             const vector<Point>& locations) const
    vector<float> descriptorsValues;
    vector<Point> locations;
    d.compute( img, descriptorsValues, Size(0,0), Size(0,0), locations);

    vector< Point >().swap(locations);
    return descriptorsValues;
}



JNIEXPORT void JNICALL Java_net_pronaya_ndkdemo_nativeInterface_prediction_1f4
  (JNIEnv * env, jobject obj, jlong testImgAddr, jlong trainingMatAddr, jlong microtextimagesideAddr, jlong microtextSideSharpenAddr,
   jint hog_feature_cnt, jint input_width, jint input_height, jint xMin, jint yMin, jint xMax, jint yMax){

    Mat& input_image_gray  = *(Mat*)testImgAddr;
    Mat& trainingMat  = *(Mat*)trainingMatAddr;
    Mat& microtextimageside  = *(Mat*)microtextimagesideAddr;
    Mat& microtextSideSharp  = *(Mat*)microtextSideSharpenAddr;

    if(input_image_gray.channels() == 3)
        cvtColor(input_image_gray, input_image_gray, COLOR_BGR2GRAY);

    int i, hog_features_no=hog_feature_cnt;  //3371760

    Mat training_mat(1, hog_features_no, DataType<float>::type);

    Mat img_main = input_image_gray.clone();
    resize(img_main, img_main, Size(input_width, input_height));
    Mat img_f1 = img_main(Rect(xMin, yMin, xMax-xMin, yMax-yMin)).clone(); //cropping feature 2 from note image
    flip(img_f1.t(), img_f1, 1);

    equalizeHist(img_f1, img_f1);
    img_f1 = get_sharpened_image4(img_f1, 3.0, 3, 5.0);
    microtextSideSharp = img_f1.clone();
    Mat img = find_edge_image4(img_f1);

     vector < float > descriptor = hog_extraction4(img);

     for(int j = 0; j < descriptor.size(); j++)
     {
        training_mat.at<float>(j) = descriptor.at(j);
     }

     trainingMat = training_mat;
     microtextimageside = img;
    // microtextSideSharp = img_f1;

     vector< float >().swap(descriptor);
     training_mat.release();
     img_main.release();
     img_f1.release();
     img.release();



    //int classifierRes=2;
    //const char *classifierPath = (*env).GetStringUTFChars(jclassifierPath, JNI_FALSE) ;
    //string strclassifierPath(classifierPath);

    //CvSVM svm;
    //svm.load(strclassifierPath); // loading
    //classifierRes = svm.predict(training_mat);
    //(*env).ReleaseStringUTFChars(jclassifierPath, classifierPath);


    //return classifierRes;

  }