package com.honeywell.poc.ocrdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity {


    EditText mResultEt;
    ImageView mPreviewIv;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String cameraPermission[];
    String storagePermission[];

    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("Click + button to insert Image");

        mResultEt = findViewById(R.id.resultEt);
        mPreviewIv = findViewById(R.id.imageIv);

        //camera permission
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    //actionbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    //handle actionbar item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.addImage){
            showImageImportDialog();
        }
        if (id == R.id.settings){
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }


    private void showImageImportDialog() {
        //items to display in dialog
        String[] items = {" Camera"," Gallery"};
        AlertDialog.Builder dialog  = new AlertDialog.Builder(this);
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0){
                    //camera option clicked
                    if (!checkCameraPermission()){
                        //camera permission not allowed, request it.
                        requestCameraPermission();
                    }else {
                        //permission allowed, take picture.
                        pickCamera();
                    }
                }
                if (i == 1){
                    //gallery option clicked
                    if (!checkStoragePermission()){
                        //Storage permission not allowed, request it.
                        requestStoragePermission();
                    }else{
                        //permission allowed, take picture.
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show();
    }


    private boolean checkCameraPermission() {
        /*  Check camera permission and return the result
            In order to get high quality image we have to save image to external storage first
            before inserting to image view.
         */
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

    }


    private boolean checkStoragePermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }


    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }


    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }


    private void pickCamera() {
        //intent to take from camera, it will also be save to storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NewPic"); //title of the picture.
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To text"); // description
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }


    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);

    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       switch (requestCode){
           case CAMERA_REQUEST_CODE:
               if (grantResults.length > 0){
                   if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                       pickCamera();
                   }else {
                       Toast.makeText(this, "permission denied.", Toast.LENGTH_SHORT).show();
                   }
               }
               break;
           case STORAGE_REQUEST_CODE:
               if (grantResults.length > 0){
                   if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                       pickGallery();
                   }else {
                       Toast.makeText(this, "permission denied.", Toast.LENGTH_SHORT).show();
                   }
               }
               break;
       }
    }

    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //got image from camera
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                Uri resultUri = result.getUri(); // get Image Uri
                //set Image to view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if (!recognizer.isOperational()){
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                }else{
                    Frame frame = new Frame.Builder().setBitmap(bitmapDrawable.getBitmap()).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    //get text from sb until there is no text
                    for (int i=0; i<items.size(); i++){
                        TextBlock myItems = items.valueAt(i);
                        sb.append(myItems.getValue());
                        sb.append("\n");
                    }
                    //set text to edit text
                    mResultEt.setText(sb.toString());
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                //if there is any error
                Exception error = result.getError();
                Toast.makeText(this, ""+error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
