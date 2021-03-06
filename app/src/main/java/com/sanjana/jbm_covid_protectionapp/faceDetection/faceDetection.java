package com.sanjana.jbm_covid_protectionapp.faceDetection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.Console;
import java.io.FileWriter;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.sanjana.jbm_covid_protectionapp.R;
import com.sanjana.jbm_covid_protectionapp.homeScreen.homeScreenActivity;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.Facing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class faceDetection extends AppCompatActivity {
    public static final String CAMERA_FACING = "com.sanjana.jbm_covid_protectionapp.CAMERA_FACEING";
    public static final String FLASH_STATUS = "com.sanjana.jbm_covid_protectionapp.FLASH_STATUS";
    private static String TAG = "StoragePermission";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static String b64;


    Button      faceDetectButton;
    ImageButton cameraRotationButton;
    ImageButton flashButton;
    public static Bitmap bitmap;
    GraphicOverlay graphicOverlay;
    CameraView cameraView;
    AlertDialog alertDialog;
    int currentFlashStatus;
    int k=0;//To get the status if activity restarts


    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
        startActivity(new Intent(faceDetection.this, homeScreenActivity.class));


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Permission();
        setContentView(R.layout.activity_face_detection);
        faceDetectButton=findViewById(R.id.detectFace);
        cameraRotationButton= findViewById(R.id.cameraRotate);
        flashButton = findViewById(R.id.flashButton);
        graphicOverlay=findViewById(R.id.graphicOverlay);
        cameraView=findViewById(R.id.cameraView);

        Toast t= Toast.makeText(faceDetection.this," Please maintain Front View of your face while detection!", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        t.show();

        alertDialog= new SpotsDialog.Builder()
                .setContext(this).setMessage("Please Wait, Processing ... ").setCancelable(false).build();

        cameraView.start();
        cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
        cameraView.setFocus(CameraKit.Constants.FOCUS_TAP);


        cameraRotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.toggleFacing();
            }
        });

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int flashStatus = cameraView.getFlash();
                if (flashStatus == 0){
                    cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);
                }
                if (flashStatus == 1 || flashStatus == 3){
                    cameraView.setFlash(CameraKit.Constants.FLASH_OFF);
                }
                Toast t= Toast.makeText(faceDetection.this," Please maintain Front View of your face while detection!", Toast.LENGTH_LONG);
                t.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
                t.show();
            }
        });


        faceDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceDetectButton.setVisibility(View.GONE);
                cameraView.captureImage();
                cameraView.stop();
                cameraView.setPinchToZoom(true);
                graphicOverlay.clear();
            }
        });
        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                alertDialog.show();
                bitmap=cameraKitImage.getBitmap();
                bitmap=Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);
                convert(bitmap);
                AnotherThread thread= new AnotherThread(bitmap);
                thread.start();
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }


    class AnotherThread extends  Thread{
        Bitmap bitmap;
        public AnotherThread(Bitmap bitmap) {
            this.bitmap= bitmap;
        }



        @Override
        public void run() {
            processFaceDetection(bitmap);

        }

        private void processFaceDetection(final Bitmap bitmap) {
            FirebaseVisionImage firebaseVisionImage= FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionFaceDetectorOptions firebaseVisionFaceDetectorOptions=  new FirebaseVisionFaceDetectorOptions.Builder().setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST).setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS).build();
            FirebaseVisionFaceDetector firebaseVisionFaceDetector= FirebaseVision.getInstance().getVisionFaceDetector(firebaseVisionFaceDetectorOptions);
            firebaseVisionFaceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                @Override
                public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                    if(firebaseVisionFaces.isEmpty())
                    {
                        Toast.makeText(faceDetection.this," Front Face not detected ! Try Again", Toast.LENGTH_SHORT).show();
                        cameraView.stop();
                        finish();
                        Intent startAgain = getIntent();
                        startAgain.putExtra(CAMERA_FACING, cameraView.getFacing());
                        startAgain.putExtra(FLASH_STATUS, cameraView.getFlash());
                        startActivity(startAgain);
                    }
                    else
                    {

                        getFaceResults(firebaseVisionFaces);
                        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss");
                        String currentTimeDateStamp = df.format(Calendar.getInstance().getTime());
                        String pathToStoredImage = saveToInternalStorage(bitmap, currentTimeDateStamp);
//                        Log.d("Path of image", pathToStoredImage);
                        Toast.makeText(faceDetection.this,"Front Face detected!", Toast.LENGTH_LONG).show();
                        cameraView.stop();

                        finish();
                        Intent i = new Intent(faceDetection.this, faceDetectionL.class);
                        i.putExtra(CAMERA_FACING, cameraView.getFacing());
                        i.putExtra(FLASH_STATUS, cameraView.getFlash());
                        startActivity(i);

                    }
                    }



            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(faceDetection.this,"Error : "+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });

        }



        private void getFaceResults(List<FirebaseVisionFace> firebaseVisionFaces) {

            int counter =0;
            for(FirebaseVisionFace face: firebaseVisionFaces)
            {
//            Rect rect=face.getBoundingBox();
//            RectOverlay rectOverlay=new RectOverlay(graphicOverlay, rect);
//            graphicOverlay.add(rectOverlay);

                FaceContourGraphic faceGraphic = new FaceContourGraphic(graphicOverlay, face);
                graphicOverlay.add(faceGraphic);
                counter=counter+1;
            }
            firebaseVisionFaces.clear();
            alertDialog.dismiss();
            //start=0;

//        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
//        getScreenshot(rootView);
        }

        private String saveToInternalStorage(Bitmap bitmapImage, String currentTimeDateStamp) {
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            // path to /data/data/yourApp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            // Create imageDir
            File myPath=new File(directory,currentTimeDateStamp + ".jpg");

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(myPath);
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    assert fos != null;
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return directory.getAbsolutePath();

        }
    }
    public static void convert(final Bitmap bitmap) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if(bitmap!=null)
               { ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                   try{
                       FileWriter fw = new FileWriter("D:\\testout.txt");
                       fw.write(Base64.encodeToString(byteArray, Base64.DEFAULT));
                       fw.close();
                   }catch(Exception e){System.out.println(e);}
                   return Base64.encodeToString(byteArray, Base64.NO_WRAP);}
                return null;

            }
            @Override
            protected void onPostExecute(String s) {
                b64 = s;
            }
        }.execute();
    }
    public static String getb64()
    {
        return b64;
    }

//    private void getScreenshot(View rootView) {
//        View screenView = rootView.getRootView();
//        screenView.setDrawingCacheEnabled(true);
//        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
//        screenView.setDrawingCacheEnabled(false);
//        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss");
//        String currentTimeDateStamp = df.format(Calendar.getInstance().getTime());
//        String pathToStoredImage = saveToInternalStorage(bitmap, currentTimeDateStamp);
//        Log.d("Path of image", pathToStoredImage);
//
////        ByteArrayOutputStream stream = new ByteArrayOutputStream();
////        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
////        byte[] byteArray = stream.toByteArray();
////
////        Bundle b = new Bundle();
////        b.putByteArray("image",byteArray);
////        GalleryFragment yourFragment = new GalleryFragment();
////        yourFragment.setArguments(b);
//    }





//    private void loadImageFromStorage(String path)
//    {
//
//        try {
//            File f=new File(path, "profile.jpg");
//            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
//            ImageView img=(ImageView)findViewById(R.id.imgPicker);
//            img.setImageBitmap(b);
//        }
//        catch (FileNotFoundException e)
//        {
//            e.printStackTrace();
//        }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intentForCameraFacingValue = getIntent();

        int cameraFacingInPreviousActivity = intentForCameraFacingValue.getIntExtra(faceDetection.CAMERA_FACING, 1);
        int flashStatusInPreviousActivity = intentForCameraFacingValue.getIntExtra(faceDetection.FLASH_STATUS, 0);
        cameraView.start();
        if(cameraFacingInPreviousActivity ==1){
            cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
        }else {
            cameraView.setFacing(CameraKit.Constants.FACING_BACK);
        }
        if(flashStatusInPreviousActivity != 0){
            cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

}




