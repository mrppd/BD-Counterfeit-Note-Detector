package net.pronaya.ndkdemo;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;
import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class n1000Activity extends AppCompatActivity {

    static {
        if(!OpenCVLoader.initDebug())
        {
            Log.i("opencv", "Opencv initialization failed");
        }
        else {
            Log.i("opencv", "Opencv initialization successful");
        }
        System.loadLibrary("registrationAndPrediction");
    }

    //public native int register_image_front(long matTemplateImageAddr, long matTestImageAddr);
    //public native int register_image_back(long matTemplateImageAddr, long matTestImageAddr);
    //public native void prediction_f1(long matTestImageAddr, long trainingMatAddr, long pimgAddr);
    //public native void prediction_f2(long matTestImageAddr, long trainingMatAddr2, long pimgAddr);
    //public native void prediction_f3(long matTestImageAddr, long trainingMatAddr2, long pimgAddr);
    //public native void prediction_f4(long matTestImageAddr, long trainingMatAddr2, long pimgAddr, long pimgSharpAddr);


    private static final int ACTION_TAKE_PHOTO_F = 1234, ACTION_TAKE_PHOTO_B = 4321, FILE_SELECT_CODE_F = 1324, FILE_SELECT_CODE_B = 4231 ;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private ImageView mImageViewFront1000, mImageViewBack1000;
    private String mCurrentFrontPhotoPath, mCurrentBackPhotoPath, templateFrontPhotoPath, templateBackPhotoPath ;
    static boolean DEBUG = true;
    public Mat rgbTemplateFrontImage;
    public Mat rgbTemplateBackImage;
    public Mat rgbTestFrontImage;
    public Mat rgbTestBackImage;
    public Mat featureWatermarkImage;
    public Mat featureLatentImage;
    public Mat featureMicrotextImage;
    public Mat featureMicrotextImage_S;
    public Mat featureMicrotextImage_S_sharp;
    public Mat training_mat_f1_1000;
    public Mat training_mat_f2_1000;
    public Mat training_mat_f3_1000;
    public Mat training_mat_f4_1000;
    private int classifier_f1_1000_res;
    private int classifier_f2_1000_res;
    private int classifier_f3_1000_res;
    private int classifier_f4_1000_res;
    private int match_info;
    public ProgressDialog progressDialog;
    public Bitmap bm_processed;
    public Button btnShowResult;
    public ImageButton btnTakePicFront, btnTakePicBack, btnOpenPicFront, btnOpenPicBack;
    public DisplayMetrics metrics;

    File imgFileDir;
    File class_file_f1_1000;
    File class_file_f2_1000;
    File class_file_f3_1000;
    File class_file_f4_1000;
    File templateFrontImageFile;
    File templateBackImageFile;
    File testsFrontImageFile;
    File testsBackImageFile;

    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private RelativeLayout relativeLayout;
    private ImageButton showFeatureWatermark;
    private ImageButton showFeatureMicrotextImage;
    private ImageButton showFeatureMicrotextSideImage;
    private ImageButton showFeatureLatentImage;
    private ImageButton showInstructionForFrontImage;
    private ImageView viewFeatureNormal;
    private ImageView viewFeatureEdge;
    private TextView popupTextForNormal;
    private TextView popupTextForEdge;
    private WebView popupWebViewInstruction;
    public nativeInterface NI;




    private Handler image_show_original_Front_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            //textView = (TextView) findViewById(R.id.textview);
            //textView.setText(mCurrentFrontPhotoPath);
            setPic(mCurrentFrontPhotoPath);
            //progressDialog.dismiss();
        }
    };

    private Handler image_show_registered_Front_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Mat tmpImg = new Mat();
            tmpImg = rgbTestFrontImage.clone();
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.what, bmOptions);
            //mImageViewBackt1000.setImageBitmap(bitmap);
            Size sz;
            if(metrics.densityDpi>=300) {
                sz = new Size((int) (tmpImg.cols() / 3), (int) (tmpImg.rows() / 3));
            }
            else {
                sz = new Size((int) (tmpImg.cols() / 4), (int) (tmpImg.rows() / 4));
            }
            //Size sz = new Size(bmOptions.outWidth, bmOptions.outHeight);
            Imgproc.resize(tmpImg, tmpImg, sz);

            bm_processed = Bitmap.createBitmap(tmpImg.cols(), tmpImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmpImg, bm_processed);
           // bm_processed = Bitmap.createScaledBitmap(bm_processed, bmOptions.outWidth, bmOptions.outHeight, true);
            mImageViewFront1000.setImageBitmap(bm_processed);

            tmpImg.release();
            //rgbTestFrontImage.release();
            file_delete_handler.sendEmptyMessage(0);

            //progressDialog.dismiss();
        }
    };

    private Handler image_show_registered_Back_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            int xMin=70, yMin=80, xMax=1674, yMax=2100;
            Rect rectCrop = new Rect(xMin, yMin, xMax-xMin, yMax-yMin);
            Mat tmpImg = rgbTestBackImage.submat(rectCrop);

            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.what, bmOptions);
            //mImageViewBackt1000.setImageBitmap(bitmap);
            Size sz;
            if(metrics.densityDpi>=300) {
                sz = new Size((int) (tmpImg.cols() / 3), (int) (tmpImg.rows() / 3));
            }
            else {
                sz = new Size((int) (tmpImg.cols() / 4), (int) (tmpImg.rows() / 4));
            }
            //Size sz = new Size(bmOptions.outWidth, bmOptions.outHeight);
            Imgproc.resize(tmpImg, tmpImg, sz);

            bm_processed = Bitmap.createBitmap(tmpImg.cols(), tmpImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmpImg, bm_processed);
            // bm_processed = Bitmap.createScaledBitmap(bm_processed, bmOptions.outWidth, bmOptions.outHeight, true);
            mImageViewBack1000.setImageBitmap(bm_processed);

            tmpImg.release();
            //rgbTestFrontImage.release();
            file_delete_handler.sendEmptyMessage(0);

            //progressDialog.dismiss();
        }
    };

    private Handler file_delete_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            //textView = (TextView) findViewById(R.id.textview);
            boolean test_front_image_1000_exists =  new File(testsFrontImageFile.getAbsolutePath()).isFile();
            if(test_front_image_1000_exists){
                testsFrontImageFile.delete();
            }
            boolean test_back_image_1000_exists =  new File(testsBackImageFile.getAbsolutePath()).isFile();
            if(test_back_image_1000_exists){
                testsBackImageFile.delete();
            }
            //mImageViewFront1000.setImageBitmap(null);
            //mImageViewFront1000.destroyDrawingCache();

            //btnShowResult.setEnabled(false);
            //progressDialog.dismiss();
        }
    };

    /*
    public static String getPath1(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public String getPath2( Uri uri) {
        String filepath = "";
        String uriPath = uri.toString();

        // Handle local file and remove url encoding
        if(uriPath.startsWith("file://")) {
            filepath = uriPath.replace("file://","");
            try {
                return URLDecoder.decode(filepath, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                filepath = cursor.getString(column_index);
            }
        }
        catch(Exception e) {
            Log.e("Path Error",e.toString());
        }
        return filepath;
    }


    public String getPath3(Context context, Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);

        cursor.moveToFirst();
        String path=null;
        try {
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        }
        catch (Exception e)
        {
            Log.e("file", "exception arised", e);
        }
        cursor.close();

        return path;
    }
*/

    public static String getPath4(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }



    private File getAlbumDir() {
        File storageDir = null;

        //File traceFile = new File(((Context)this).getExternalFilesDir(null), "TraceFile.txt");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            //storageDir = mAlbumStorageDirFactory.getAlbumStorageDir("MyCameraApp");
            storageDir = new File(((Context)this).getExternalFilesDir(null),"MyCameraApp");

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }


    private void setPic(String photoPath) {


		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //bmOptions.inJustDecodeBounds = true;
        //BitmapFactory.decodeFile(photoPath, bmOptions);
        //int photoW = bmOptions.outWidth;
        //int photoH = bmOptions.outHeight;

        Bitmap bitmapA = BitmapFactory.decodeResource(getResources(), R.drawable.what, bmOptions);
		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        bitmap = Bitmap.createScaledBitmap(bitmap, bmOptions.outWidth, bmOptions.outHeight, true);
		/* Associate the Bitmap to the ImageView */
        mImageViewFront1000.setImageBitmap(bitmap);


        mImageViewBack1000.setImageBitmap(bitmapA);

        //mVideoUri = null;
        //mImageViewFront1000.setVisibility(View.VISIBLE);
        //mVideoView.setVisibility(View.INVISIBLE);
    }


    private void saveToSD( String filePath, int resourceID ) {
        try {
            InputStream inf = getResources().openRawResource(resourceID);
            //FileInputStream fin = new FileInputStream(inf.toString());
            BufferedInputStream in = new BufferedInputStream(inf);
            FileOutputStream out = new FileOutputStream(filePath);
            XZInputStream xzIn = new XZInputStream(in);
            final byte[] buffer = new byte[8192];
            int n = 0;
            while (-1 != (n = xzIn.read(buffer))) {
                out.write(buffer, 0, n);

                // textView.setText(Integer.toString(ii));
                //ii=ii+1;
            }
            out.close();
            xzIn.close();
            Log.d("Decompress", "successful");
        }
        catch(Exception e) {
            Log.e("Decompress", "exception arised", e);
        }
        /*
        InputStream inf = getResources().openRawResource(R.raw.n1000_f1_classifier_xml);
        FileOutputStream out = new FileOutputStream(filePath);
        byte[] buff = new byte[1024];
        int read = 0;

        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
        */
    }


    public int predict_f1_1000_latent_image(File classifierFile){
        training_mat_f1_1000 = new Mat();
        NI.prediction_f1_1000(rgbTestFrontImage.getNativeObjAddr(), training_mat_f1_1000.getNativeObjAddr(), featureLatentImage.getNativeObjAddr());
        CvSVM svm = new CvSVM();
        svm.load(classifierFile.getAbsolutePath()); // loading
        int classifierRes = (int)svm.predict(training_mat_f1_1000);

        svm.clear();
        training_mat_f1_1000.release();
        return classifierRes;
    }

    public int predict_f2_1000_watermark_image(File classifierFile){
        training_mat_f2_1000 = new Mat();
        NI.prediction_f2_1000(rgbTestFrontImage.getNativeObjAddr(), training_mat_f2_1000.getNativeObjAddr(), featureWatermarkImage.getNativeObjAddr());
        CvSVM svmf2 = new CvSVM();
        svmf2.load(classifierFile.getAbsolutePath()); // loading
        int classifierRes = (int)svmf2.predict(training_mat_f2_1000);

        svmf2.clear();
        training_mat_f2_1000.release();
        return classifierRes;
    }

    public int predict_f3_1000_microtext_inmage(File classifierFile){
        training_mat_f3_1000 = new Mat();
        NI.prediction_f3_1000(rgbTestBackImage.getNativeObjAddr(), training_mat_f3_1000.getNativeObjAddr(), featureMicrotextImage.getNativeObjAddr());
        CvSVM svmf3 = new CvSVM();
        svmf3.load(classifierFile.getAbsolutePath()); // loading
        int classifierRes = (int)svmf3.predict(training_mat_f3_1000);

        svmf3.clear();
        training_mat_f3_1000.release();
        return classifierRes;
    }

    public int predict_f4_1000_microtext_inmage(File classifierFile){
        training_mat_f4_1000 = new Mat();
        NI.prediction_f4_1000(rgbTestBackImage.getNativeObjAddr(), training_mat_f4_1000.getNativeObjAddr(), featureMicrotextImage_S.getNativeObjAddr(), featureMicrotextImage_S_sharp.getNativeObjAddr());
        CvSVM svmf4 = new CvSVM();
        svmf4.load(classifierFile.getAbsolutePath()); // loading
        int classifierRes = (int)svmf4.predict(training_mat_f4_1000);

        svmf4.clear();
        training_mat_f4_1000.release();
        return classifierRes;
    }


    private void showFileChooser(int actionCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select an appropriate image"), actionCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }


    }


    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch(actionCode) {
            case ACTION_TAKE_PHOTO_F: {
                //File f = null;

                //try {
                //f = setUpPhotoFile();
                mCurrentFrontPhotoPath = testsFrontImageFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(testsFrontImageFile));
                //} catch (IOException e) {
                //    e.printStackTrace();
                //f = null;
                //mCurrentPhotoPath = null;
                // }
                break;
            }
            case ACTION_TAKE_PHOTO_B: {
                //File f = null;

                //try {
                //f = setUpPhotoFile();
                mCurrentBackPhotoPath = testsBackImageFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(testsBackImageFile));
                //} catch (IOException e) {
                //    e.printStackTrace();
                //f = null;
                //mCurrentPhotoPath = null;
                // }
                break;
            }
            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }


    Button.OnClickListener mTakeFrontPicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_F);
                }
            };


    Button.OnClickListener mTakeBackPicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };


    private void setPopupResults(TextView popupTextResult, int classifier_res )
    {
        if(classifier_res!=0 && classifier_res==1) {
            popupTextResult.setText("OK");
            popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_green));
        }
        else if(classifier_res!=0 && classifier_res==-1) {
            popupTextResult.setText("NOT OK");
            popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
        }
        else {
            popupTextResult.setText("---");
            popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_yellow));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_n1000);

        getSupportActionBar().setTitle(R.string.n1000Title);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        classifier_f1_1000_res = 0;
        classifier_f2_1000_res = 0;
        classifier_f3_1000_res = 0;
        classifier_f4_1000_res = 0;
        match_info=0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
        NI = new nativeInterface();

        metrics = getResources().getDisplayMetrics();
        Log.d("devicedpi", Integer.toString(metrics.densityDpi));

        btnShowResult = (Button) findViewById(R.id.btnShowResultN1000);
        mImageViewFront1000 = (ImageView) findViewById(R.id.imgViewFront1000) ;
        mImageViewBack1000 = (ImageView) findViewById(R.id.imgViewBack1000) ;
        rgbTestFrontImage = new Mat();
        rgbTestBackImage = new Mat();
        featureWatermarkImage = new Mat();
        featureLatentImage = new Mat();
        featureMicrotextImage = new Mat();
        featureMicrotextImage_S = new Mat();
        featureMicrotextImage_S_sharp =new Mat();


        btnOpenPicFront = (ImageButton) findViewById(R.id.btnOpenFolder1);
        btnTakePicFront = (ImageButton) findViewById(R.id.btnOpenCamera1);
        btnOpenPicBack = (ImageButton) findViewById(R.id.btnOpenFolder2);
        btnTakePicBack = (ImageButton) findViewById(R.id.btnOpenCamera2);


        //String path = "/sdcard/Pictures/qq.jpg";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                progressDialog = ProgressDialog.show(n1000Activity.this, "Please Wait", "Initializing...");
            }
        });

        //runnig under new thread
        new Thread()
        {
            public void run()
            {
                try
                {
                    //sleep(9500);
                    //Needed file information
                    imgFileDir = getAlbumDir();
                    class_file_f1_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f1_classifier.xml");
                    class_file_f2_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f2_classifier_v3.xml");
                    class_file_f3_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f3_classifier_v2.xml");
                    class_file_f4_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f4_classifier.xml");
                    templateFrontImageFile = new File(imgFileDir.getPath() + File.separator + "front1000.jpg");
                    templateBackImageFile = new File(imgFileDir.getPath() + File.separator + "back1000.jpg");
                    testsFrontImageFile = new File(imgFileDir.getPath() + File.separator + "testFrontImage.jpg");
                    testsBackImageFile = new File(imgFileDir.getPath() + File.separator + "testBackImage.jpg");
                    templateFrontPhotoPath = templateFrontImageFile.getAbsolutePath();
                    templateBackPhotoPath = templateBackImageFile.getAbsolutePath();
                    mCurrentFrontPhotoPath = testsFrontImageFile.getAbsolutePath();
                    mCurrentBackPhotoPath = testsBackImageFile.getAbsolutePath();

                    /*
                    boolean test_front_image_1000_exists =  new File(testsFrontImageFile.getAbsolutePath()).isFile();
                    boolean test_back_image_1000_exists =  new File(testsFrontImageFile.getAbsolutePath()).isFile();

                    if(!test_front_image_1000_exists) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //btnShowResult.setEnabled(false);
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //btnShowResult.setEnabled(true);
                            }
                        });
                    }

                    if(!test_back_image_1000_exists) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //btnShowResult.setEnabled(false);
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //btnShowResult.setEnabled(true);
                            }
                        });
                    }
                    */

                    //Saving classifier file from resource to phone SD card (Picture/MyCameraApp/)
                    boolean class_file_f1_1000_exists =  new File(class_file_f1_1000.getAbsolutePath()).isFile();
                    boolean class_file_f2_1000_exists =  new File(class_file_f2_1000.getAbsolutePath()).isFile();
                    boolean class_file_f3_1000_exists =  new File(class_file_f3_1000.getAbsolutePath()).isFile();
                    boolean class_file_f4_1000_exists =  new File(class_file_f4_1000.getAbsolutePath()).isFile();
                    boolean template_front_image_1000_exists =  new File(templateFrontImageFile.getAbsolutePath()).isFile();
                    boolean template_back_image_1000_exists =  new File(templateBackImageFile.getAbsolutePath()).isFile();
                    if(!class_file_f1_1000_exists || !class_file_f2_1000_exists || !class_file_f3_1000_exists || !class_file_f4_1000_exists || !template_front_image_1000_exists || !template_back_image_1000_exists)
                    {
                        saveToSD(class_file_f1_1000.getAbsolutePath(), R.raw.n1000_f1_classifier);
                        saveToSD(class_file_f2_1000.getAbsolutePath(), R.raw.n1000_f2_classifier_v3);
                        saveToSD(class_file_f3_1000.getAbsolutePath(), R.raw.n1000_f3_classifier_v2);
                        saveToSD(class_file_f4_1000.getAbsolutePath(), R.raw.n1000_f4_classifier);
                        saveToSD(templateFrontImageFile.getAbsolutePath(), R.raw.front1000_b);
                        saveToSD(templateBackImageFile.getAbsolutePath(), R.raw.back1000half);

                        Log.i("Classifiers", "one or many file/s do/does not exist and creating from resource");
                    }
                    else
                    {
                        Log.i("Classifiers", "file exist");
                    }

                   // rgbTemplateFrontImage = Highgui.imread(templateFrontPhotoPath);



                   /*
                    if (DEBUG)
                        Log.d("picsize", "loadedImage: " + "chans: " + rgbTestImage.channels()
                                + ", (" + rgbTestImage.width() + ", " + rgbTestImage.height() + ")");

                    if (rgbLoadedImage.width() > 0) {
                         Imgproc.cvtColor(rgbLoadedImage, rgbLoadedImage, Imgproc.COLOR_BGR2GRAY);
                    }
                    */

                }
                catch (Exception e)
                {
                    Log.e("tag",e.getMessage());
                }
                // dismiss the progressdialog
                progressDialog.dismiss();
            }
        }.start();


        setBtnListenerOrDisable(
                btnTakePicFront,
                mTakeFrontPicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        setBtnListenerOrDisable(
                btnTakePicBack,
                mTakeBackPicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );


        btnOpenPicFront.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFileChooser(FILE_SELECT_CODE_F);
            }
        });

        btnOpenPicBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showFileChooser(FILE_SELECT_CODE_B);
            }
        });




        relativeLayout = (RelativeLayout) findViewById(R.id.layoutN1000);

        //codes for showing instructions in popup
        showInstructionForFrontImage = (ImageButton) findViewById(R.id.btnInstruction1);

        showInstructionForFrontImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_instruction, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.95);
                int height = (int)(dm.heightPixels*0.75);


                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                popupWebViewInstruction = (WebView) container.findViewById(R.id.webViewInstruction);
                popupWebViewInstruction.loadUrl("file:///android_asset/Some_ways.html");


                ImageButton backButton= (ImageButton) container.findViewById(R.id.btnInstructionBack);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

            }
        });


        //codes for showing latent image in popup
        showFeatureLatentImage = (ImageButton) findViewById(R.id.btnFeature1);
        showFeatureLatentImage.setEnabled(false);

        showFeatureLatentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_feature, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.9);
                int height = (int)(dm.heightPixels*0.7);


                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                popupTextForNormal = (TextView) container.findViewById(R.id.popupHead1);
                popupTextForNormal.setText("Latent Image (Normal)");
                popupTextForEdge = (TextView) container.findViewById(R.id.popupHead2);
                popupTextForEdge.setText("Latent Image (Filtered)");

                viewFeatureNormal  = (ImageView) container.findViewById(R.id.imgFeatureNormal);
                viewFeatureNormal.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                viewFeatureEdge  = (ImageView) container.findViewById(R.id.imgFeatureEdge);
                viewFeatureEdge.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                int xMin=1216, yMin=1156, xMax=2380, yMax=1314;
                Rect rectCrop = new Rect(xMin, yMin, xMax-xMin, yMax-yMin);
                Mat img_f1_watermark = rgbTestFrontImage.submat(rectCrop);
                Bitmap tmpBitmap = Bitmap.createBitmap(img_f1_watermark.cols(), img_f1_watermark.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_f1_watermark, tmpBitmap);
                viewFeatureNormal.setImageBitmap(tmpBitmap);
                img_f1_watermark.release();

                tmpBitmap = Bitmap.createBitmap(featureLatentImage.cols(), featureLatentImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(featureLatentImage, tmpBitmap);
                viewFeatureEdge.setImageBitmap(tmpBitmap);


                container.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent){
                        viewFeatureNormal.setImageDrawable(null);
                        //viewFeatureNormal.setImageBitmap(null);
                        viewFeatureNormal.destroyDrawingCache();
                        viewFeatureEdge.setImageDrawable(null);
                        viewFeatureEdge.destroyDrawingCache();
                        popupWindow.dismiss();
                        return true;
                    }
                });

            }
        });

        //codes for showing watermark in popup
        showFeatureWatermark = (ImageButton) findViewById(R.id.btnFeature2);
        showFeatureWatermark.setEnabled(false);
        showFeatureWatermark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_feature, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.9);
                int height = (int)(dm.heightPixels*0.7);

                popupTextForNormal = (TextView) container.findViewById(R.id.popupHead1);
                popupTextForNormal.setText("Watermark (Normal)");
                popupTextForEdge = (TextView) container.findViewById(R.id.popupHead2);
                popupTextForEdge.setText("Watermark (Filtered)");

                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                viewFeatureNormal  = (ImageView) container.findViewById(R.id.imgFeatureNormal);
                viewFeatureEdge  = (ImageView) container.findViewById(R.id.imgFeatureEdge);

                int xMin=2434, yMin=502, xMax=3012, yMax=1144;
                Rect rectCrop = new Rect(xMin, yMin, xMax-xMin, yMax-yMin);
                Mat img_f1_watermark = rgbTestFrontImage.submat(rectCrop);
                Bitmap tmpBitmap = Bitmap.createBitmap(img_f1_watermark.cols(), img_f1_watermark.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_f1_watermark, tmpBitmap);
                viewFeatureNormal.setImageBitmap(tmpBitmap);
                img_f1_watermark.release();

                tmpBitmap = Bitmap.createBitmap(featureWatermarkImage.cols(), featureWatermarkImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(featureWatermarkImage, tmpBitmap);
                viewFeatureEdge.setImageBitmap(tmpBitmap);


                container.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent){
                        viewFeatureNormal.setImageDrawable(null);
                        //viewFeatureNormal.setImageBitmap(null);
                        viewFeatureNormal.destroyDrawingCache();
                        viewFeatureEdge.setImageDrawable(null);
                        viewFeatureEdge.destroyDrawingCache();
                        popupWindow.dismiss();
                        return true;
                    }
                });

            }
        });



        //codes for mocrotext image in popup
        showFeatureMicrotextImage = (ImageButton) findViewById(R.id.btnFeature3);
        showFeatureMicrotextImage.setEnabled(false);

        showFeatureMicrotextImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_feature, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.9);
                int height = (int)(dm.heightPixels*0.7);


                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                popupTextForNormal = (TextView) container.findViewById(R.id.popupHead1);
                popupTextForNormal.setText("Micro-text Image (Equalized)");
                popupTextForEdge = (TextView) container.findViewById(R.id.popupHead2);
                popupTextForEdge.setText("Micro-text Image (Filtered)");

                viewFeatureNormal  = (ImageView) container.findViewById(R.id.imgFeatureNormal);
                viewFeatureEdge  = (ImageView) container.findViewById(R.id.imgFeatureEdge);

                int xMin=340, yMin=1580, xMax=1420, yMax=1910;
                Rect rectCrop = new Rect(xMin, yMin, xMax-xMin, yMax-yMin);
                Mat img_f1_microtext = rgbTestBackImage.submat(rectCrop);
                Imgproc.equalizeHist(img_f1_microtext, img_f1_microtext);
                Bitmap tmpBitmap = Bitmap.createBitmap(img_f1_microtext.cols(), img_f1_microtext.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_f1_microtext, tmpBitmap);
                viewFeatureNormal.setImageBitmap(tmpBitmap);
                img_f1_microtext.release();

                tmpBitmap = Bitmap.createBitmap(featureMicrotextImage.cols(), featureMicrotextImage.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(featureMicrotextImage, tmpBitmap);
                viewFeatureEdge.setImageBitmap(tmpBitmap);



                container.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent){
                        viewFeatureNormal.setImageDrawable(null);
                        //viewFeatureNormal.setImageBitmap(null);
                        viewFeatureNormal.destroyDrawingCache();
                        viewFeatureEdge.setImageDrawable(null);
                        viewFeatureEdge.destroyDrawingCache();
                        popupWindow.dismiss();
                        return true;
                    }
                });

            }
        });

        //codes for mocrotext_side image in popup
        showFeatureMicrotextSideImage = (ImageButton) findViewById(R.id.btnFeature4);
        showFeatureMicrotextSideImage.setEnabled(false);

        showFeatureMicrotextSideImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_feature, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.9);
                int height = (int)(dm.heightPixels*0.7);


                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                popupTextForNormal = (TextView) container.findViewById(R.id.popupHead1);
                popupTextForNormal.setText("Micro-text(Side) Image (Sharpen)");
                popupTextForEdge = (TextView) container.findViewById(R.id.popupHead2);
                popupTextForEdge.setText("Micro-text(Side) Image (Filtered)");

                viewFeatureNormal  = (ImageView) container.findViewById(R.id.imgFeatureNormal);
                viewFeatureEdge  = (ImageView) container.findViewById(R.id.imgFeatureEdge);


                Bitmap tmpBitmap = Bitmap.createBitmap(featureMicrotextImage_S_sharp.cols(), featureMicrotextImage_S_sharp.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(featureMicrotextImage_S_sharp, tmpBitmap);
                viewFeatureNormal.setImageBitmap(tmpBitmap);

                tmpBitmap = Bitmap.createBitmap(featureMicrotextImage_S.cols(), featureMicrotextImage_S.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(featureMicrotextImage_S, tmpBitmap);
                viewFeatureEdge.setImageBitmap(tmpBitmap);



                container.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent){
                        viewFeatureNormal.setImageDrawable(null);
                        //viewFeatureNormal.setImageBitmap(null);
                        viewFeatureNormal.destroyDrawingCache();
                        viewFeatureEdge.setImageDrawable(null);
                        viewFeatureEdge.destroyDrawingCache();
                        popupWindow.dismiss();
                        return true;
                    }
                });

            }
        });


        //codes for result in popup

        btnShowResult.setEnabled(false);

        btnShowResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup_results, null);

                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int Width=(int)(dm.widthPixels*0.95);
                int height = (int)(dm.heightPixels*0.8);

                popupWindow = new PopupWindow(container, Width, height, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER, 0, 0);

                //your code for content here
                TextView popupTextResult = (TextView) container.findViewById(R.id.textLatentRes);
                setPopupResults(popupTextResult, classifier_f1_1000_res);

                popupTextResult = (TextView) container.findViewById(R.id.textWatermarkRes);
                setPopupResults(popupTextResult, classifier_f2_1000_res);

                popupTextResult = (TextView) container.findViewById(R.id.textMicrotextBRes);
                setPopupResults(popupTextResult, classifier_f3_1000_res);

                popupTextResult = (TextView) container.findViewById(R.id.textMicrotextSRes);
                setPopupResults(popupTextResult, classifier_f4_1000_res);

                int result_percentage = (int)Math.round(((float)(classifier_f1_1000_res>0?1:0)/3 + (float)(classifier_f2_1000_res>0?1:0)/3
                        + ((float)(classifier_f3_1000_res>0?1:0)*(float)0.5)/3 + ((float)(classifier_f4_1000_res>0?1:0)*(float)0.5)/3)*100);

                popupTextResult = (TextView) container.findViewById(R.id.textVerdictRes);
                if(result_percentage>50) {
                    popupTextResult.setText("ORIGINAL");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_green));
                }
                else
                {
                    popupTextResult.setText("COUNTERFEIT");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                }

                popupTextResult = (TextView) container.findViewById(R.id.textPercentageRes);
                popupTextResult.setText(Integer.toString(result_percentage) + "%");
                if(result_percentage>50 && result_percentage<=70) {
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_yellow));
                }
                else if(result_percentage>70 && result_percentage<=85) {
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_yellow_green));
                }
                else if(result_percentage>85) {
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_green));
                }
                else {
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                }


                //Partial Result
                popupTextResult = (TextView) container.findViewById(R.id.textVerdictRes);
                if(classifier_f1_1000_res!=0 && classifier_f3_1000_res==0 && result_percentage>40) {
                    popupTextResult.setText("ORIGINAL");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_green));
                    popupTextResult = (TextView) container.findViewById(R.id.textPercentageRes);
                    popupTextResult.setText("Partial");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_yellow));
                }
                else if(classifier_f1_1000_res!=0 && classifier_f3_1000_res==0 && result_percentage<40) {
                    popupTextResult.setText("COUNTERFEIT");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                    popupTextResult = (TextView) container.findViewById(R.id.textPercentageRes);
                    popupTextResult.setText("Partial");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                }

                popupTextResult = (TextView) container.findViewById(R.id.textVerdictRes);
                if(classifier_f1_1000_res==0 && classifier_f3_1000_res!=0 && result_percentage>30) {
                    popupTextResult.setText("ORIGINAL");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_green));
                    popupTextResult = (TextView) container.findViewById(R.id.textPercentageRes);
                    popupTextResult.setText("Partial");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_yellow));
                }
                else if(classifier_f1_1000_res==0 && classifier_f3_1000_res!=0 && result_percentage<30) {
                    popupTextResult.setText("COUNTERFEIT");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                    popupTextResult = (TextView) container.findViewById(R.id.textPercentageRes);
                    popupTextResult.setText("Partial");
                    popupTextResult.setTextColor(ContextCompat.getColor(n1000Activity.this, R.color.bright_red));
                }


                //////////////////////////////

                ImageButton backButton= (ImageButton) container.findViewById(R.id.btnBack);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        popupWindow.dismiss();

                    }
                });

            }
        });


        match_info = getIntent().getExtras().getInt("match_info");
        if(match_info>0)
            frontImageRelatedCalculation();
    }


    //This function is called inside onActivityResult twice. It is responsible for acquiring,
    // registering front image file with support other native and non native function.
    private void frontImageRelatedCalculation()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                progressDialog = ProgressDialog.show(n1000Activity.this, "Please wait a moment","Processing Image", true);
            }
        });

        //runnig under new thread
        new Thread() {
            public void run() {
                try {
                    featureLatentImage.release();
                    featureWatermarkImage.release();
                    rgbTestFrontImage.release();
                    rgbTemplateFrontImage = Highgui.imread(templateFrontPhotoPath);
                    rgbTestFrontImage = Highgui.imread(mCurrentFrontPhotoPath, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                    if(match_info==0)
                        match_info = NI.register_image_front_1000(rgbTemplateFrontImage.getNativeObjAddr(), rgbTestFrontImage.getNativeObjAddr());

                    if(match_info>=20) {
                        match_info=0;
                        image_show_registered_Front_handler.sendEmptyMessage(0);

                        classifier_f1_1000_res = predict_f1_1000_latent_image(class_file_f1_1000);
                        classifier_f2_1000_res = predict_f2_1000_watermark_image(class_file_f2_1000);

                        //Temporary results
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                //TextView res = (TextView) findViewById(R.id.textFront);
                                //res.setText(Integer.toString(classifier_f1_1000_res) + " " + Integer.toString(classifier_f2_1000_res));
                                showFeatureWatermark.setEnabled(true);
                                showFeatureLatentImage.setEnabled(true);
                                btnShowResult.setEnabled(true);
                            }
                        });
                    }
                    else
                    {
                        match_info=0;
                        classifier_f1_1000_res=0;
                        classifier_f2_1000_res=0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.what);
                                mImageViewFront1000.setImageBitmap(bitmap);
                                showFeatureWatermark.setEnabled(false);
                                showFeatureLatentImage.setEnabled(false);
                                btnShowResult.setEnabled(false);
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                Toast toast = Toast.makeText(n1000Activity.this, "Poor Image Quality! Image cannot be acquired. Try again.", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }

                    //image_show_original_Front_handler.sendEmptyMessage(0);
                    rgbTemplateFrontImage.release();
                    //rgbTestFrontImage.release();

                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }
                // dismiss the progressdialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
                        progressDialog.dismiss();
                    }
                });
            }
        }.start();
    }


    //This function is called inside onActivityResult twice. It is responsible for acquiring,
    // registering back image file with support other native and non native function.
    private void backImageRelatedCalculation()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                progressDialog = ProgressDialog.show(n1000Activity.this, "Please wait a moment","Processing Image", true);
            }
        });

        //runnig under new thread
        new Thread() {
            public void run() {
                try {
                    featureMicrotextImage_S_sharp.release();
                    featureMicrotextImage_S.release();
                    featureMicrotextImage.release();
                    rgbTestBackImage.release();;
                    rgbTemplateBackImage = Highgui.imread(templateBackPhotoPath);
                    rgbTestBackImage = Highgui.imread(mCurrentBackPhotoPath);
                    int match_info = NI.register_image_back_1000(rgbTemplateBackImage.getNativeObjAddr(), rgbTestBackImage.getNativeObjAddr());
                    if(match_info>=20) {
                        image_show_registered_Back_handler.sendEmptyMessage(0);

                        classifier_f3_1000_res = predict_f3_1000_microtext_inmage(class_file_f3_1000);
                        classifier_f4_1000_res = predict_f4_1000_microtext_inmage(class_file_f4_1000);

                        //Temporary results
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                showFeatureMicrotextSideImage.setEnabled(true);
                                showFeatureMicrotextImage.setEnabled(true);
                                btnShowResult.setEnabled(true);
                                //TextView res = (TextView) findViewById(R.id.textBack);
                                //res.setText(Integer.toString(classifier_f3_1000_res) + " " + Integer.toString(classifier_f4_1000_res));
                            }
                        });
                    }
                    else
                    {
                        classifier_f3_1000_res=0;
                        classifier_f4_1000_res=0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.what);
                                mImageViewBack1000.setImageBitmap(bitmap);
                                showFeatureMicrotextSideImage.setEnabled(false);
                                showFeatureMicrotextImage.setEnabled(false);
                                btnShowResult.setEnabled(false);
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                Toast toast = Toast.makeText(n1000Activity.this, "Poor Image Quality! Image cannot be acquired. Try again.", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        });
                    }

                    //image_show_original_Front_handler.sendEmptyMessage(0);
                    rgbTemplateBackImage.release();
                    //rgbTestFrontImage.release();

                } catch (Exception e) {
                    Log.e("tag", e.getMessage());
                }
                // dismiss the progressdialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
                        progressDialog.dismiss();
                    }
                });
            }
        }.start();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_F: {
                if (resultCode == RESULT_OK) {

                    frontImageRelatedCalculation();
                }
                break;
            } // ACTION_TAKE_PHOTO_F
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {

                    backImageRelatedCalculation();
                }
                break;
            } // ACTION_TAKE_PHOTO_B
            case FILE_SELECT_CODE_F: {
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("file", "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    path = getPath4(n1000Activity.this, uri);
                    Log.d("file", "File Path: " + path);

                    if(path!=null)
                        mCurrentFrontPhotoPath = path;

                    frontImageRelatedCalculation();

                }
                break;
            }
            case FILE_SELECT_CODE_B: {
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("file", "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    path = getPath4(n1000Activity.this, uri);
                    Log.d("file", "File Path: " + path);

                    if(path!=null)
                        mCurrentBackPhotoPath = path;

                    backImageRelatedCalculation();

                }
                break;
            }
        } // switch
    }

/*
    @Override
    public void onPause(){
        super.onPause();
        //finish();
        file_delete_handler.sendEmptyMessage(0);
    //    rgbTemplateFrontImage.release();
//        rgbTestFrontImage.release();
    }
*/
    @Override
    public void onStop(){
        super.onStop();



        file_delete_handler.sendEmptyMessage(0);
        //    rgbTemplateFrontImage.release();
//        rgbTestFrontImage.release();
    }

    @Override
    protected void onDestroy() {
        //android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();


/*
        try {
            rgbTemplateFrontImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            rgbTemplateBackImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            rgbTestFrontImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            rgbTestBackImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            featureWatermarkImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            featureLatentImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            featureMicrotextImage.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            featureMicrotextImage_S.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }

        try {
            featureMicrotextImage_S_sharp.release();
        }
        catch (Exception e)
        {
            Log.e("release_exception", e.getMessage());
        }
*/

        file_delete_handler.sendEmptyMessage(0);
     //   rgbTemplateFrontImage.release();
      //  rgbTestFrontImage.release();
        Runtime.getRuntime().gc();
        System.gc();
    }


    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(
            ImageButton btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        }
        else {

            btn.setClickable(false);
        }
    }
}
