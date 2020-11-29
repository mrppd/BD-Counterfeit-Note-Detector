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


Mat find_edge_image(Mat img_f1, int note)
{
    Mat kernel;
    if(note==1000)
    {
        kernel = (Mat_<char>(3, 3) << 0, 1, 2,
                                     -1, 0, 1,
                                     -2, -1, 0);
    }
    if(note==500)
    {
        kernel = (Mat_<char>(3, 3) << 2,  1,  0,
                                          1,  0, -1,
                                          0, -1, -2);
    }
    filter2D(img_f1, img_f1, img_f1.depth(), kernel);

    Mat binaryMat(img_f1.size(), img_f1.type());
    //Apply thresholding
    threshold(img_f1, binaryMat, 125, 255, cv::THRESH_BINARY);

    kernel.release();
    binaryMat.release();
    return img_f1;
}

vector< float > hog_extraction(Mat img )
{
    HOGDescriptor d(Size(64, 128), Size(16,16), Size(8,8), Size(8,8), 9);
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
    d.compute( img, descriptorsValues, Size(8,8), Size(0,0), locations);

    vector< Point >().swap(locations);
    return descriptorsValues;
}



JNIEXPORT void JNICALL Java_net_pronaya_ndkdemo_nativeInterface_prediction_1f1
  (JNIEnv * env, jobject obj, jlong testImgAddr, jlong trainingMatAddr, jlong latentimageAddr,
   jint hog_feature_cnt, jint note, jint input_width, jint input_height, jint xMin, jint yMin, jint xMax, jint yMax){

    Mat& input_image_gray  = *(Mat*)testImgAddr;
    Mat& trainingMat  = *(Mat*)trainingMatAddr;
    Mat& latentimage  = *(Mat*)latentimageAddr;

    if(input_image_gray.channels() == 3)
        cvtColor(input_image_gray, input_image_gray, COLOR_BGR2GRAY);

    int i, hog_features_no=hog_feature_cnt;

    Mat training_mat(1, hog_features_no, DataType<float>::type);

    Mat img_main = input_image_gray.clone();
    resize(img_main, img_main, Size(input_width, input_height) );
    //cvtColor(img_raw, img, CV_RGB2GRAY);
    //img(cv::Rect(xMin,yMin,xMax-xMin,yMax-yMin)).copyTo(croppedImg);
    //Rect myROI(10, 10, 100, 100);
     Mat img_f1 = img_main(Rect(xMin,yMin,xMax-xMin,yMax-yMin)); //cropping feature 1 from note image

     Mat img = find_edge_image(img_f1, note);

     vector < float > descriptor = hog_extraction(img);

     for(int j = 0; j < descriptor.size(); j++)
     {
        training_mat.at<float>(j) = descriptor.at(j);
     }

     trainingMat = training_mat;
     latentimage=img;

     training_mat.release();
     img_main.release();
     img_f1.release();
     img.release();

     vector< float >().swap(descriptor);

    //int classifierRes=2;
    //const char *classifierPath = (*env).GetStringUTFChars(jclassifierPath, JNI_FALSE) ;
    //string strclassifierPath(classifierPath);

    //CvSVM svm;
    //svm.load(strclassifierPath); // loading
    //classifierRes = svm.predict(training_mat);
    //(*env).ReleaseStringUTFChars(jclassifierPath, classifierPath);


    //return classifierRes;


    /*
    Mat tmp;
    double otsu_thresh_val = threshold(input_image_gray, tmp, 0, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);

    double high_thresh_val = otsu_thresh_val, lower_thresh_val = otsu_thresh_val * 0.5;
    Canny( input_image_gray, input_image_gray, lower_thresh_val, high_thresh_val );
    tmp.release();
    */
    //equalizeHist( input_image_gray, input_image_gray );
  }