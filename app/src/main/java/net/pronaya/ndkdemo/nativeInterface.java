package net.pronaya.ndkdemo;



public class nativeInterface {

    static {
        System.loadLibrary("registrationAndPrediction");
    }
    public native int register_image_front(long matTemplateImageAddr, long matTestImageAddr);
    public native int register_image_back(long matTemplateImageAddr, long matTestImageAddr);
    public native void prediction_f1(long matTestImageAddr, long trainingMatAddr, long pimgAddr,
                                     int hog_f_cnt, int note, int img_w, int img_h, int crop_x_min, int crop_y_min, int crop_x_max, int crop_y_max);
    public native void prediction_f2(long matTestImageAddr, long trainingMatAddr2, long pimgAddr,
                                     int hog_f_cnt, int note, int img_w, int img_h, int crop_x_min, int crop_y_min, int crop_x_max, int crop_y_max);
    public native void prediction_f3(long matTestImageAddr, long trainingMatAddr2, long pimgAddr,
                                     int hog_f_cnt, int img_w, int img_h, int crop_x_min, int crop_y_min, int crop_x_max, int crop_y_max);
    public native void prediction_f4(long matTestImageAddr, long trainingMatAddr2, long pimgAddr, long pimgSharpAddr,
                                     int hog_f_cnt, int img_w, int img_h, int crop_x_min, int crop_y_min, int crop_x_max, int crop_y_max);

    private int hog_features_cnt, note, input_width, input_height, xMin, yMin, xMax, yMax;

    public void  prediction_f1_1000(long matTestImageAddr, long trainingMatAddr, long pimgAddr)
    {
        hog_features_cnt=2086560;
        note=1000;
        input_width=3264;
        input_height=1406;
        xMin=1216;
        yMin=1156;
        xMax=2380;
        yMax=1314;
        prediction_f1(matTestImageAddr, trainingMatAddr, pimgAddr, hog_features_cnt, note, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public void  prediction_f2_1000(long matTestImageAddr, long trainingMatAddr2, long pimgAddr)
    {
        hog_features_cnt=823284;
        note=1000;
        input_width=3264;
        input_height=1406;
        xMin=2434;
        yMin=502;
        xMax=3012;
        yMax=1144;
        prediction_f2(matTestImageAddr, trainingMatAddr2, pimgAddr, hog_features_cnt, note, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public void  prediction_f3_1000(long matTestImageAddr, long trainingMatAddr2, long pimgAddr)
    {
        hog_features_cnt=2515968;
        input_width=2840;
        input_height=2172;
        xMin=340;
        yMin=1580;
        xMax=1420;
        yMax=1910;
        prediction_f3(matTestImageAddr, trainingMatAddr2, pimgAddr, hog_features_cnt, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public void  prediction_f4_1000(long matTestImageAddr, long trainingMatAddr2, long pimgAddr, long pimgSharpAddr)
    {
        hog_features_cnt=3371760;
        input_width=2840;
        input_height=2172;
        xMin=335;
        yMin=170;
        xMax=424;
        yMax=2080;
        prediction_f4(matTestImageAddr, trainingMatAddr2, pimgAddr, pimgSharpAddr, hog_features_cnt, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public int register_image_back_1000(long matTemplateImageAddr, long matTestImageAddr)
    {
        return register_image_front(matTemplateImageAddr, matTestImageAddr);
    }

    public int register_image_front_1000(long matTemplateImageAddr, long matTestImageAddr)
    {
        return register_image_back(matTemplateImageAddr, matTestImageAddr);
    }




    public void  prediction_f1_500(long matTestImageAddr, long trainingMatAddr, long pimgAddr)
    {
        hog_features_cnt=1564920;
        note=500;
        input_width=3264;
        input_height=1372;
        xMin=1224;
        yMin=1140;
        xMax=2390;
        yMax=1290;
        prediction_f1(matTestImageAddr, trainingMatAddr, pimgAddr, hog_features_cnt, note, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public void  prediction_f2_500(long matTestImageAddr, long trainingMatAddr2, long pimgAddr)
    {
        hog_features_cnt=13827240;
        note=500;
        input_width=3264;
        input_height=1372;
        xMin=2510;
        yMin=432;
        xMax=3040;
        yMax=1048;
        prediction_f2(matTestImageAddr, trainingMatAddr2, pimgAddr, hog_features_cnt, note, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public void  prediction_f3_500(long matTestImageAddr, long trainingMatAddr2, long pimgAddr)
    {
        hog_features_cnt=1330560;
        input_width=2170;
        input_height=1852;
        xMin=334;
        yMin=1444;
        xMax=1100;
        yMax=1728;
        prediction_f3(matTestImageAddr, trainingMatAddr2, pimgAddr, hog_features_cnt, input_width, input_height, xMin, yMin, xMax, yMax);
    }

    public int register_image_back_500(long matTemplateImageAddr, long matTestImageAddr)
    {
        return register_image_front(matTemplateImageAddr, matTestImageAddr);
    }

    public int register_image_front_500(long matTemplateImageAddr, long matTestImageAddr)
    {
        return register_image_back(matTemplateImageAddr, matTestImageAddr);
    }

}
