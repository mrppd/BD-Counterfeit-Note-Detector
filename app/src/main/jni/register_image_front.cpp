//
// Created by Pronaya on 9/5/2016.
//

#include <jni.h>
#include "net_pronaya_ndkdemo_nativeInterface.h"

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



int image_registration_f(Mat template_image_gray, Mat input_image_gray, Mat &result_image,
                        int least_good_match=20, float r_t=0.2, float r_i=0.2, int fthreshold=45)
{
    if(template_image_gray.channels() == 3)
            cvtColor(template_image_gray, template_image_gray, COLOR_BGR2GRAY);
        if(input_image_gray.channels() == 3)
            cvtColor(input_image_gray, input_image_gray, COLOR_BGR2GRAY);


            Size template_size = template_image_gray.size();
            Mat input_image_gray_or = input_image_gray.clone();
            int x_t = (template_image_gray.cols*r_t);
            int y_t = (template_image_gray.rows*r_t);
            int x_i = (input_image_gray.cols*r_i);
            int y_i = (input_image_gray.rows*r_i);
            resize(template_image_gray, template_image_gray, Size(x_t, y_t));
            resize(input_image_gray, input_image_gray, Size(x_i, y_i));


            //int fthreshold = 45;
            // extractor; //or
            FREAK extractor;
            Mat descriptors_template, descriptors_input;
            vector < KeyPoint > keypoints_template, keypoints_input;

            //detector.detect(template_image_gray, keypoints_template);
            FAST(template_image_gray, keypoints_template, fthreshold);
            extractor.compute(template_image_gray, keypoints_template, descriptors_template);

            //detector.detect(input_image_gray, keypoints_input);
            FAST(input_image_gray, keypoints_input, fthreshold);
            extractor.compute(input_image_gray, keypoints_input, descriptors_input);


            BFMatcher matcher(NORM_HAMMING);
            vector< vector< DMatch > > matches;
            matcher.knnMatch(descriptors_input, descriptors_template, matches, 2);

            vector< DMatch > good_matches;
            for (int i = 0; i < matches.size(); ++i)
            {
                const float ratio = 0.8; // As in Lowe's paper; can be tuned
                if (matches[i][0].distance < ratio * matches[i][1].distance)
                {
                    good_matches.push_back(matches[i][0]);
                }
            }


            vector<Point2f> template_pts, input_pts;
            for( int i = 0; i < good_matches.size(); i++ )
            {
                //-- Get the keypoints from the good matches
                //input_pts.push_back(Point2f(keypoints_input[good_matches[i].queryIdx ].pt.x, keypoints_input[ good_matches[i].queryIdx].pt.y));
                //template_pts.push_back(Point2f(keypoints_template[good_matches[i].trainIdx ].pt.x, keypoints_template[ good_matches[i].trainIdx].pt.y));
                input_pts.push_back(keypoints_input[good_matches[i].queryIdx ].pt);
                template_pts.push_back(keypoints_template[good_matches[i].trainIdx ].pt);
            }


            if(good_matches.size()<least_good_match)
            {
                input_image_gray_or.release();
                descriptors_template.release();
                descriptors_input.release();

                vector< KeyPoint >().swap(keypoints_template);
                vector< KeyPoint >().swap(keypoints_input);
                vector< Point2f >().swap(template_pts);
                vector< Point2f >().swap(input_pts);
                vector< vector< DMatch >  >().swap(matches);

                return good_matches.size();
            }


            Mat status;
            Mat H = findHomography(input_pts, template_pts, CV_RANSAC, 3, status);

            vector< DMatch > inliers;
            for(size_t i = 0; i < good_matches.size(); i++)
            {
                if((unsigned int)status.at<char>(i) != 0)
                {
                    inliers.push_back(good_matches[i]);
                }
            }

            if(inliers.size()<least_good_match)
            {
                input_image_gray_or.release();
                descriptors_template.release();
                descriptors_input.release();
                status.release();
                H.release();

                vector< KeyPoint >().swap(keypoints_template);
                vector< KeyPoint >().swap(keypoints_input);
                vector< Point2f >().swap(template_pts);
                vector< Point2f >().swap(input_pts);
                vector< DMatch >().swap(good_matches);
                vector< vector< DMatch >  >().swap(matches);

                return inliers.size();
            }

            vector< Point2f > template_pts2, input_pts2;
            for(int i=0; i<inliers.size(); i++)
            {
                template_pts2.push_back(Point2f(keypoints_template[ inliers[i].trainIdx ].pt.x/r_t, keypoints_template[ inliers[i].trainIdx ].pt.y/r_t));
                input_pts2.push_back(Point2f(keypoints_input[ inliers[i].queryIdx ].pt.x/r_i, keypoints_input[ inliers[i].queryIdx ].pt.y/r_i));
            }


            Mat H2 = findHomography(input_pts2, template_pts2, CV_RANSAC);


            Mat result;
            warpPerspective(input_image_gray_or, result, H2, template_size);

            result_image = result.clone();
            int match_info =  inliers.size();

            //free up the memories

            input_image_gray_or.release();
            descriptors_template.release();
            descriptors_input.release();
            status.release();
            H.release();
            H2.release();
            result.release();

            vector< KeyPoint >().swap(keypoints_template);
            vector< KeyPoint >().swap(keypoints_input);
            vector< Point2f >().swap(template_pts);
            vector< Point2f >().swap(input_pts);
            vector< Point2f >().swap(template_pts2);
            vector< Point2f >().swap(input_pts2);
            vector< DMatch >().swap(good_matches);
            vector< DMatch >().swap(inliers);
            vector< vector< DMatch >  >().swap(matches);

            return match_info;
}




JNIEXPORT jint JNICALL Java_net_pronaya_ndkdemo_nativeInterface_register_1image_1front
  (JNIEnv * env, jobject obj, jlong templateImgAddr, jlong testImgAddr){

    Mat& template_image_gray  = *(Mat*)templateImgAddr;
    Mat& input_image_gray  = *(Mat*)testImgAddr;

    int res = image_registration_f(template_image_gray, input_image_gray, input_image_gray, 20);


    return res;

    //return (*env).NewStringUTF( "Hello from JNI2 and cv");
  }