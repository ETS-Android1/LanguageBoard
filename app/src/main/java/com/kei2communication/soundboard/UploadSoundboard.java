package com.kei2communication.soundboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UploadSoundboard extends AppCompatActivity {


    private static final int PICK_IMG = 0;
    private ArrayList<Soundboard> soundboards;
    private Uri image;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private MyApplication mApp;
    TextView belowText, aboveText;
    EditText sbEditText;
    Spinner sbSpinner;
    Button choose, send, newSoundboard, editSoundboard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_soundboard);

        mApp = (MyApplication)getApplication();
        soundboards = mApp.getSoundboards();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        belowText = findViewById(R.id.below_text);
        aboveText = findViewById(R.id.above_text);
        newSoundboard = findViewById(R.id.newSB);
        editSoundboard = findViewById(R.id.editSB);
        choose = findViewById(R.id.choose);
        send = findViewById(R.id.send);
        send.setEnabled(false);
        sbEditText = findViewById(R.id.soundboard_category_ET);
        sbSpinner = findViewById(R.id.soundboard_category_dropdown);


        //Change view for editing current soundboard
        editSoundboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupEditView();
            }
        });

        //Change view for creating a new soundboard
        newSoundboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupNewView();
            }
        });

        //Pick image
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(v);
            }
        });

        //Upload Image / create soundboard
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make sure EditText is not empty
                if(sbEditText.getVisibility() == View.VISIBLE) {
                    if (sbEditText.getText().toString().isEmpty()) {
                        Toast.makeText(UploadSoundboard.this, "Please enter a language board name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                send.setEnabled(false);
                upload(getName().toLowerCase());
            }
        });

    }

    public void setupEditView(){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, mApp.getUserSoundboardNames());
        sbSpinner.setAdapter(adapter);
        newSoundboard.setBackgroundTintList(getResources().getColorStateList(R.color.GREY));
        newSoundboard.setTextColor(getResources().getColor(R.color.DARK_GREY));
        editSoundboard.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        editSoundboard.setTextColor(getResources().getColor(R.color.WHITE));
        aboveText.setText("Edit A Current Language Board");
        ((TextView)findViewById(R.id.name_title)).setText("Select Langauge Board: ");
        sbSpinner.setVisibility(View.VISIBLE);
        sbEditText.setVisibility(View.GONE);
        send.setText("Upload Language Board Image");
    }


    public void setupNewView(){
        newSoundboard.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        newSoundboard.setTextColor(getResources().getColor(R.color.WHITE));
        editSoundboard.setBackgroundTintList(getResources().getColorStateList(R.color.GREY));
        editSoundboard.setTextColor(getResources().getColor(R.color.DARK_GREY));
        aboveText.setText("Create A New Language Board");
        ((TextView)findViewById(R.id.name_title)).setText("Language Board Name: ");
        sbSpinner.setVisibility(View.GONE);
        sbEditText.setVisibility(View.VISIBLE);
        send.setText("Create New Language Board");
    }

    /**
     * User selects desired image for soundboard cover
     */
    public void choose(View view) {
        //Pick images from system file viewer
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(intent, PICK_IMG);
    }


    @Override
    //Process selected image
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMG) {
            if (resultCode == RESULT_OK) {
                if(data.getData() != null) {
                    image  = data.getData();
                    belowText.setVisibility(View.VISIBLE);
                    belowText.setText("Image Selected: " + data.getData().getPath());
                    send.setEnabled(true);
                }
            }
        }
    }


    /**
     * Upload image chosen by user to FirebaseStorage
     * Image stored under "images/[UID]/[category]/[date]"
     */
    public void upload(String category) {
        progressDialog.show();
        final StorageReference imageFolder =  FirebaseStorage.getInstance().getReference();

        //Date for filename
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");

        final StorageReference imagename = imageFolder.child("images/" + mAuth.getUid() + "/" + category + "/"+sdf.format(date));

        //Add image to firestore
        imagename.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imagename.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        sendToDatabase(String.valueOf(uri));
                    }
                });
            }
        });
    }


    /**
     * Add soundboard to the database
     * Send image chosen by user and display name from the textbox
     */
    private void sendToDatabase(final String url) {
        String user = mAuth.getUid();
        final String name = getName();

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("imageURL", url);
        hashMap.put("name", name);

        db.collection("users").document(user)
                .collection("soundboards").document(name.toLowerCase())
                .set(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();

                ArrayList<SBCard> tempCards = new ArrayList<>();
                if(sbEditText.getVisibility() == View.GONE){
                    int pos = mApp.findPosition(name);
                    tempCards = soundboards.get(pos).getSoundboardCards();
                    soundboards.remove(pos);
                }
                soundboards.add(new Soundboard(name, url, tempCards));

                //AlertDialog prompt to go back or add another
                completeMessage();
            }
        });

    }

    //Return the name from the edittext or the spinner selection
    public String getName(){
        if(sbEditText.getVisibility() == View.GONE)
            return  sbSpinner.getSelectedItem().toString();
        else if (sbSpinner.getVisibility() == View.GONE)
            return  sbEditText.getText().toString();
        else
            return null;
    }

    //Prompt AlertDialog asking if the user wants to go back or do more
    public void completeMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Successful");
        builder.setMessage("Would you like to create/edit another language board?");

        builder.setNegativeButton("Return Home", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Change view back
                sbEditText.setText("");
                belowText.setText("Select an image to upload and save your language board");
                image = null;
            }
        });

        builder.show();

    }
}