package com.swathi.ocr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText etResult;
    ImageView imgView;
    EditText etRTranslate;
    Button  btnTranslate;
    StringBuilder sourceText;
private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLEY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1001;

    String cameraPermission[];
    String storagePermission[];

    Bitmap imageBitmap;

    Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etRTranslate=findViewById(R.id.etRTranslate);
        etResult=findViewById(R.id.etResult);
        imgView=findViewById(R.id.imgView);
        btnTranslate=findViewById(R.id.btnTranslate);
        ActionBar actionbar=getSupportActionBar();

//camera permission
        cameraPermission=new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
         btnTranslate.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 translateText();

             }
         });
    }
    private void translateText(){
        int langCode;
        langCode= FirebaseTranslateLanguage.EN;

        FirebaseTranslatorOptions options= new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(langCode)
                .setTargetLanguage(FirebaseTranslateLanguage.FR)
                .build();
      final FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance()
                .getTranslator(options);
        FirebaseModelDownloadConditions conditions =new FirebaseModelDownloadConditions.Builder()
                .build();
      translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
              translator.translate(String.valueOf(sourceText)).addOnSuccessListener(new OnSuccessListener<String>() {
                  @Override
                  public void onSuccess(String s) {
                      etRTranslate.setText(s);
                  }
              });
          }
      });
    }
//ActionBar Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);

        return true;
    }
    //Handle actionBAr item Clicks

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if (id ==R.id.addImage){
                showImageImportDialog();
        }
        if(id==R.id.settings){
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        //items to display in dialog
        String[] items={"Camera" , "Gallery"};
        AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which ==0){
                    //camera option clicked
                    if(!checkCameraPermission()){
                        //camera permission not allowed ,request it
                        requestCameraPermission();
                    }
                    else{
                        //permission allowed ,take picture
                        pickCamera();

                    }

                }
                if(which==1)
            //gallery option clicked
                    if(!checkStoragePermission()){
                        //camera permission not allowed ,request it
                        requestStoragePermission();
                    }
                    else{
                        //permission allowed ,take picture
                        pickGallery();

                    }

            }
        });
        dialog.create().show();

    }

    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent= new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityIfNeeded(intent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickCamera() {
        //intent to take image from camera ,it will also be save to storage to get high quality image
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NEWPIC");//title of the the picture
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To text");//description
        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityIfNeeded(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,IMAGE_PICK_GALLEY_CODE);


    }

    private boolean checkStoragePermission() {
        boolean result=ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE )==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA )==(PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE )==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    //handle permission Result

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLEY_CODE) {

                    CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON)
                            .start(this);


            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri).setGuidelines(CropImageView.Guidelines.ON).start(this);
            }
        }
        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                //set image to imageView
                imgView.setImageURI(resultUri);
                //get drawable for text recogaination
                BitmapDrawable bitmapDrawable = (BitmapDrawable)imgView.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();
                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    //set text to edit text
                    etResult.setText((sb.toString()));
                    StringBuilder trans = new StringBuilder();
                    trans=sb;
                    sourceText=trans;
                   // etRTranslate.setText((trans.toString()));
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //if there is any error show
                Exception error = result.getError();
                Toast.makeText(this, " " + error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}