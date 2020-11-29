package net.pronaya.ndkdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvSVM;
import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MyActivity extends AppCompatActivity {

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


    private static final int ACTION_TAKE_PHOTO_B = 1234;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    public nativeInterface NI;
    public ProgressDialog progressDialog;
    public Button btnTakePic;
    public Bitmap bm_processed;
    File imgFileDir, testsFrontImageFile;
    File class_file_f1_500, class_file_f2_500, class_file_f3_500, templateFrontImageFile500, templateBackImageFile500;
    File class_file_f1_1000, class_file_f2_1000, class_file_f3_1000, class_file_f4_1000, templateFrontImageFile1000, templateBackImageFile1000;
    private String mCurrentFrontPhotoPath, templateFrontPhotoPath500, templateFrontPhotoPath1000 ;







    private File getAlbumDir() {
        File storageDir = null;

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
/*
    private void setPic() {

		// There isn't enough memory to open up more than a couple camera photos
		// So pre-scale the target bitmap into which the file is decoded

		// Get the size of the ImageView
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		// Get the size of the image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		// Figure out which way needs to be reduced less
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

		// Set bitmap options to scale the image decode target
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		// Decode the JPEG file into a Bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		// Associate the Bitmap to the ImageView
        mImageView.setImageBitmap(bitmap);
        //mVideoUri = null;
        mImageView.setVisibility(View.VISIBLE);
        //mVideoView.setVisibility(View.INVISIBLE);
    }
*/
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
    }



    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch(actionCode) {
            case ACTION_TAKE_PHOTO_B: {
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
            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }

        NI = new nativeInterface();

        Button btn500 = (Button) findViewById(R.id.btn500);
        btn500.setOnClickListener(new View.OnClickListener(){
            @Override
            //On click function
            public void onClick(View view) {
                //Create the intent to start another activity
                Intent intent = new Intent(view.getContext(), n500Activity.class);
                intent.putExtra("note", 500);
                intent.putExtra("match_info", 0);
                startActivity(intent);
            }
        });

        Button btn1000 = (Button) findViewById(R.id.btn1000);
        btn1000.setOnClickListener(new View.OnClickListener(){
            @Override
            //On click function
            public void onClick(View view) {
                //Create the intent to start another activity
                Intent intent = new Intent(view.getContext(), n1000Activity.class);
                intent.putExtra("note", 1000);
                intent.putExtra("match_info", 0);
                startActivity(intent);
            }
        });



        btnTakePic = (Button) findViewById(R.id.btnTakePic);


        //String path = "/sdcard/Pictures/qq.jpg";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                progressDialog = ProgressDialog.show(MyActivity.this, "Please Wait", "Initializing for the first time. It may take a minute.");
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

                    class_file_f1_500 = new File(imgFileDir.getPath() + File.separator + "n500_f1_classifier.xml");
                    class_file_f2_500 = new File(imgFileDir.getPath() + File.separator + "n500_f2_classifier_v1.xml");
                    class_file_f3_500 = new File(imgFileDir.getPath() + File.separator + "n500_f3_classifier_v2_2.xml");
                    templateFrontImageFile500 = new File(imgFileDir.getPath() + File.separator + "front500.jpg");
                    templateBackImageFile500 = new File(imgFileDir.getPath() + File.separator + "back500.jpg");

                    //Saving classifier file from resource to phone SD card (Data/.../MyCameraApp/)
                    boolean class_file_f1_500_exists =  new File(class_file_f1_500.getAbsolutePath()).isFile();
                    boolean class_file_f2_500_exists =  new File(class_file_f2_500.getAbsolutePath()).isFile();
                    boolean class_file_f3_500_exists =  new File(class_file_f3_500.getAbsolutePath()).isFile();
                    boolean template_front_image_500_exists =  new File(templateFrontImageFile500.getAbsolutePath()).isFile();
                    boolean template_back_image_500_exists =  new File(templateBackImageFile500.getAbsolutePath()).isFile();
                    if(!class_file_f1_500_exists || !class_file_f2_500_exists || !class_file_f3_500_exists || !template_front_image_500_exists || !template_back_image_500_exists)
                    {
                        saveToSD(class_file_f1_500.getAbsolutePath(), R.raw.n500_f1_classifier);
                        saveToSD(class_file_f2_500.getAbsolutePath(), R.raw.n500_f2_classifier_v1);
                        saveToSD(class_file_f3_500.getAbsolutePath(), R.raw.n500_f3_classifier_v2_2);
                        saveToSD(templateFrontImageFile500.getAbsolutePath(), R.raw.front500_2_b);
                        saveToSD(templateBackImageFile500.getAbsolutePath(), R.raw.back500half_b);

                        Log.i("Classifiers", "one or many file/s do/does not exist and creating from resource");
                    }
                    else
                    {
                        Log.i("Classifiers", "file exist");
                    }


                    class_file_f1_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f1_classifier.xml");
                    class_file_f2_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f2_classifier_v3.xml");
                    class_file_f3_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f3_classifier_v2.xml");
                    class_file_f4_1000 = new File(imgFileDir.getPath() + File.separator + "n1000_f4_classifier.xml");
                    templateFrontImageFile1000 = new File(imgFileDir.getPath() + File.separator + "front1000.jpg");
                    templateBackImageFile1000 = new File(imgFileDir.getPath() + File.separator + "back1000.jpg");

                    //Saving classifier file from resource to phone SD card (Data/.../MyCameraApp/)
                    boolean class_file_f1_1000_exists =  new File(class_file_f1_1000.getAbsolutePath()).isFile();
                    boolean class_file_f2_1000_exists =  new File(class_file_f2_1000.getAbsolutePath()).isFile();
                    boolean class_file_f3_1000_exists =  new File(class_file_f3_1000.getAbsolutePath()).isFile();
                    boolean class_file_f4_1000_exists =  new File(class_file_f4_1000.getAbsolutePath()).isFile();
                    boolean template_front_image_1000_exists =  new File(templateFrontImageFile1000.getAbsolutePath()).isFile();
                    boolean template_back_image_1000_exists =  new File(templateBackImageFile1000.getAbsolutePath()).isFile();
                    if(!class_file_f1_1000_exists || !class_file_f2_1000_exists || !class_file_f3_1000_exists || !class_file_f4_1000_exists || !template_front_image_1000_exists || !template_back_image_1000_exists)
                    {
                        saveToSD(class_file_f1_1000.getAbsolutePath(), R.raw.n1000_f1_classifier);
                        saveToSD(class_file_f2_1000.getAbsolutePath(), R.raw.n1000_f2_classifier_v3);
                        saveToSD(class_file_f3_1000.getAbsolutePath(), R.raw.n1000_f3_classifier_v2);
                        saveToSD(class_file_f4_1000.getAbsolutePath(), R.raw.n1000_f4_classifier);
                        saveToSD(templateFrontImageFile1000.getAbsolutePath(), R.raw.front1000_b);
                        saveToSD(templateBackImageFile1000.getAbsolutePath(), R.raw.back1000half);

                        Log.i("Classifiers", "one or many file/s do/does not exist and creating from resource");
                    }
                    else
                    {
                        Log.i("Classifiers", "file exist");
                    }


                    testsFrontImageFile = new File(imgFileDir.getPath() + File.separator + "testFrontImage.jpg");
                    templateFrontPhotoPath500 = templateFrontImageFile500.getAbsolutePath();
                    templateFrontPhotoPath1000 = templateFrontImageFile1000.getAbsolutePath();
                    mCurrentFrontPhotoPath = testsFrontImageFile.getAbsolutePath();


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
                btnTakePic,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );





    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // This code will always run on the UI thread, therefore is safe to modify UI elements.
                            progressDialog = ProgressDialog.show(MyActivity.this, "Please wait a moment","Processing Image", true);
                        }
                    });

                    //runnig under new thread
                    new Thread() {
                        public void run() {
                            try {

                                //Do your code here
                                Mat rgbTemplateFrontImage = Highgui.imread(templateFrontPhotoPath1000);
                                Mat rgbTestFrontImage = Highgui.imread(mCurrentFrontPhotoPath);
                                Mat rgbTestFrontImage1000 = rgbTestFrontImage.clone();
                                int match_info_1000 = NI.register_image_front_1000(rgbTemplateFrontImage.getNativeObjAddr(), rgbTestFrontImage1000.getNativeObjAddr());

                                Mat rgbTestFrontImage500 = rgbTestFrontImage.clone();
                                rgbTemplateFrontImage = Highgui.imread(templateFrontPhotoPath500);
                                int match_info_500 = NI.register_image_front_500(rgbTemplateFrontImage.getNativeObjAddr(), rgbTestFrontImage500.getNativeObjAddr());

                                if(match_info_1000>=20 || match_info_500>=20) {

                                    if (match_info_1000 > match_info_500) {
                                        bm_processed = Bitmap.createBitmap(rgbTestFrontImage1000.cols(), rgbTestFrontImage1000.rows(), Bitmap.Config.ARGB_8888);
                                        Utils.matToBitmap(rgbTestFrontImage1000, bm_processed);
                                    } else {
                                        bm_processed = Bitmap.createBitmap(rgbTestFrontImage500.cols(), rgbTestFrontImage500.rows(), Bitmap.Config.ARGB_8888);
                                        Utils.matToBitmap(rgbTestFrontImage500, bm_processed);
                                    }


                                    FileOutputStream out = null;
                                    try {
                                        out = new FileOutputStream(mCurrentFrontPhotoPath);
                                        bm_processed.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        try {
                                            if (out != null) {
                                                out.close();
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }


                                    rgbTemplateFrontImage.release();
                                    rgbTestFrontImage.release();
                                    rgbTestFrontImage1000.release();
                                    rgbTestFrontImage500.release();

                                    if (match_info_1000 > match_info_500) {
                                        Intent intent = new Intent(MyActivity.this, n1000Activity.class);
                                        intent.putExtra("note", 1000);
                                        intent.putExtra("match_info", match_info_1000);
                                        startActivity(intent);
                                    } else {
                                        Intent intent = new Intent(MyActivity.this, n500Activity.class);
                                        intent.putExtra("note", 500);
                                        intent.putExtra("match_info", match_info_500);
                                        startActivity(intent);
                                    }

                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                            Toast toast = Toast.makeText(MyActivity.this, "Poor Image Capture/Quality! Image cannot be acquired. Try again.", Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                                            toast.show();
                                        }
                                    });
                                }

                                //image_show_original_handler.sendEmptyMessage(0);

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
                break;
            } // ACTION_TAKE_PHOTO_B
        } // switch
    }




/*
    @Override
    public void onResume(){
        super.onResume();
        //finish();
        startActivity(getIntent());
    }

*/
    @Override
    public void onPause(){
        super.onPause();
        //finish();

        //file_delete_handler.sendEmptyMessage(0);
    }


    @Override
    protected void onDestroy() {
        //android.os.Process.killProcess(android.os.Process.myPid());

        super.onDestroy();
        //file_delete_handler.sendEmptyMessage(0);

        Runtime.getRuntime().gc();
        System.gc();
        /*
            finish();
        bitmapImage = null;
        scaledBitmap = null;
        super.onDestroy();
        Runtime.getRuntime().gc();
        System.gc()
        if(scaledBitmap!=null)
        {
            scaledBitmap.recycle();
            scaledBitmap=null;
        }
        */

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
            Button btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(
                    getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }
}
