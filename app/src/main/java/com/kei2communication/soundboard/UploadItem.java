package com.kei2communication.soundboard;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

public class UploadItem extends AppCompatActivity {


    private static final int PICK_IMG = 0;
    private Uri image;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private String sbCategory;
    private Intent intent;
    private int position;
    private MyApplication mApp;
    TextView belowText, aboveText, categoryText;
    EditText itemEditText;
    Spinner itemSpinner;
    Button choose, send, newItem, editItem;
    ArrayList<SBCard> sbCards;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        mApp = ((MyApplication)getApplication());
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        belowText = findViewById(R.id.below_text);
        aboveText = findViewById(R.id.above_text);
        categoryText = findViewById(R.id.soundboard_category);
        newItem = findViewById(R.id.newItem);
        editItem = findViewById(R.id.editItem);
        choose = findViewById(R.id.choose);
        send = findViewById(R.id.send);
        send.setEnabled(false);
        itemEditText = findViewById(R.id.soundboard_item_ET);
        itemSpinner = findViewById(R.id.soundboard_item_dropdown);

        intent = getIntent();
        sbCategory = intent.getStringExtra("category");
        categoryText.setText(sbCategory);
        position = intent.getIntExtra("soundboardPosition", 0);
        sbCards = mApp.getSoundboards().get(position).getSoundboardCards();

        this.setTitle("Customize " + sbCategory);

        //Change view to edit current item
        editItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] names = getNames(sbCards);
                if(names != null){
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, names);
                    itemSpinner.setAdapter(adapter);
                }
                newItem.setBackgroundTintList(getResources().getColorStateList(R.color.GREY));
                newItem.setTextColor(getResources().getColor(R.color.DARK_GREY));
                editItem.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                editItem.setTextColor(getResources().getColor(R.color.WHITE));
                aboveText.setText("Edit A Current Language Board Item");
                ((TextView)findViewById(R.id.name_title)).setText("Select Item: ");
                itemSpinner.setVisibility(View.VISIBLE);
                itemEditText.setVisibility(View.GONE);
                send.setText("Upload Item Image");
            }
        });

        //Change view for adding new item
        newItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newItem.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                newItem.setTextColor(getResources().getColor(R.color.WHITE));
                editItem.setBackgroundTintList(getResources().getColorStateList(R.color.GREY));
                editItem.setTextColor(getResources().getColor(R.color.DARK_GREY));
                aboveText.setText("Add A New Language Board Item");
                ((TextView)findViewById(R.id.name_title)).setText("Item Name: ");
                itemSpinner.setVisibility(View.GONE);
                itemEditText.setVisibility(View.VISIBLE);
                send.setText("Add New Item");
            }
        });


        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose(v);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make sure EditText is not empty
                if(itemEditText.getVisibility() == View.VISIBLE) {
                    if (itemEditText.getText().toString().isEmpty()) {
                        Toast.makeText(UploadItem.this, "Please enter language board name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                send.setEnabled(false);
                upload();
            }
        });
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
    public void upload() {
        progressDialog.show();
        final StorageReference imageFolder =  FirebaseStorage.getInstance().getReference();

        //Date for filename
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");

        final StorageReference imagename = imageFolder.child("images/" + mAuth.getUid() + "/" + sbCategory.toLowerCase() + "/"+sdf.format(date));

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
                .collection("soundboards").document(sbCategory.toLowerCase())
                .collection("soundboard_cards").document(name.toLowerCase())
                .set(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if(itemEditText.getVisibility() == View.GONE)
                    sbCards.remove(itemSpinner.getSelectedItemPosition());
                sbCards.add(new SBCard(name,url, sbCategory));
                completeMessage();
            }
        });

    }

    //Get name from view - either edittext or spinner selection
    public String getName(){
        if(itemEditText.getVisibility() == View.GONE)
            return  itemSpinner.getSelectedItem().toString();
        else if (itemSpinner.getVisibility() == View.GONE)
            return  itemEditText.getText().toString();
        else
            return null;
    }

    //Return array of names from list of SBCards
    public String[] getNames(ArrayList<SBCard> items){
        if(items.size() == 0)
            return null;

        String[] names = new String[items.size()];
        for(int i = 0; i < items.size(); i++){
            names[i] = items.get(i).getName();
        }
        return names;
    }


    //Prompt AlertDialog asking if the user wants to go back or do more
    public void completeMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Successful");
        builder.setMessage("Would you like to create/edit another language board?");

        //Return to soundboard page
        builder.setNegativeButton("Return Home", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        //Stay and continue adding/editing soundboard items
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                itemEditText.setText("");
                image = null;
                belowText.setText("Select an image to upload and save your language board item");
            }
        });
        builder.show();
    }
}