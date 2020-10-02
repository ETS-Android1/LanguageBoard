package com.kei2communication.soundboard;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

public class MainActivity extends AppCompatActivity {


    private final int CODE = 100;
    private MyApplication mApp;
    private ArrayList<Soundboard> soundboards;
    private ArrayList<String> presetSoundboardNames;
    private ArrayList<String> presetSoundboardImages;
    private ArrayList<String> userSoundboardNames;
    private ArrayList<String> userSoundboardImages;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore db;
    RecyclerView rv;
    MainRVAdapter myAdapter;
    MutableLiveData<Integer> listen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check for audio and file writing permissions
        checkPermissions();

        //Check for first time tutorial
        mApp = ((MyApplication)getApplication());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean("start_key", false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("start_key", Boolean.TRUE);
            edit.commit();
            mApp.setTutorial(true);
        }

        //Anonymous sign in
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    mUser = mAuth.getCurrentUser();
                    Log.d("Anonymous Signin", "Successful Signin. UID: " + mUser.getUid());

                    //if the user is new, create a user document and set uid
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", mUser.getUid());
                    db.collection("users").document(mUser.getUid()).set(data, SetOptions.merge());

                    //initiate variables
                    setup();

                    //Load preset soundboards from database
                    getSoundboardCategories("preset", presetSoundboardNames, presetSoundboardImages);
                }
                else
                    Log.e("Anonymous Signin", "Anon Signin Failed: " + task.getException());
            }
        });


        listen = new MutableLiveData<>();
        listen.setValue(0);
        //Triggered when the value of 'listen' is changed
        //Triggered when the soundboards are done loading in the fillSoundboards method
        listen.observe(this, new androidx.lifecycle.Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                //Once the preset soundboards are loaded in
                //Start loading custom user soundboards
                if(integer == 1) {
                    getSoundboardCategories(mAuth.getCurrentUser().getUid(), userSoundboardNames, userSoundboardImages);
                }
                //hide loading and update view after ALL soundboards are loaded in
                if(listen.getValue() == 2) {
                    doneLoading();
                }
            }
        });


        //add_button listener
        FloatingActionButton addSoundboard = findViewById(R.id.add_button);
        addSoundboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UploadSoundboard.class);
                intent.putExtra("names", userSoundboardNames);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, CODE);
            }
        });
    }

    /**
     * Make a list of soundboard names and images from Firebase
     * List of names represents all the soundboard categories under a specific user
     */
    public void getSoundboardCategories(final String user, final ArrayList<String> names, final ArrayList<String> images){
        //Get the soundboard data from the database and make a list of soundboards
        db.collection("users").document(user).collection("soundboards").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List <DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                //Make a list of names and imageURLs of all documents in the 'soundboards' collection of the user
                //each document represents a soundboards category
                for(int i = 0; i < list.size(); i++){
                    //Add soundboard names to a preset or a user list
                    if(user.equals("preset"))
                        mApp.addPresetName(list.get(i).get("name").toString());
                    else
                        mApp.addUserSoundboardName(list.get(i).get("name").toString());

                    names.add(list.get(i).get("name").toString());
                    images.add(list.get(i).get("imageURL").toString());
                }
                //After the list is complete, get the soundboards items for each of the soundboards
                if(names.size() > 0)
                    fillSoundboards(user, names, images);
                //after preset soundboards are loaded in
                //only if there are NO user soundboards
                //hide the loading icon and update the view
                else {
                    doneLoading();
                }
            }
        });
    }


    /**
     *  Make a list of soundboards
     *  Each soundboard has a display name, image, and a list of SBCards
     *  Soundboard items (SBCards) received from database
     */
    public void fillSoundboards(String user, final ArrayList<String> names, final ArrayList<String> images){
        //Loop through every document (soundboard item) in each of the soundboards
        //Create a Soundboard with a list of SBCards from each document in every soundboard
        for(int x = 0; x < names.size(); x++){
            final int y = x;
            db.collection("users").document(user).collection("soundboards")
                    .document(names.get(y).toLowerCase())
                    .collection("soundboard_cards").get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    List <DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                    ArrayList<SBCard> cards = new ArrayList<>();
                    //loop through documents creating list of cards
                    for(int i = 0; i < list.size(); i++){
                        cards.add(new SBCard(list.get(i).get("name").toString(), list.get(i).get("imageURL").toString(), names.get(y)));
                    }
                    //create a new soundboards with the list of cards
                    soundboards.add(new Soundboard(names.get(y), images.get(y), cards));

                    //Triggered once soundboards are loaded in
                    if(soundboards.size() == names.size() || soundboards.size() == names.size() + presetSoundboardNames.size()) {
                        listen.setValue(listen.getValue()+1);
                    }
                }
            });
        }
    }

    //request audio and write permisisons
    public void checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 10);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void setup(){
        //Declare arraylists
        soundboards = mApp.getSoundboards();
        presetSoundboardNames = new ArrayList<>();
        presetSoundboardImages = new ArrayList<>();
        userSoundboardNames = new ArrayList<>();
        userSoundboardImages = new ArrayList<>();
    }

    //After all soundboards are loaded
    //sort soundboard list, hide loading icon, and update the view
    public void doneLoading(){
        Collections.sort(soundboards); //sort alphabetically
        findViewById(R.id.loadingPanel).setVisibility(View.GONE); //hide loading icon

        //setup recyclerview
        rv = (RecyclerView)findViewById(R.id.rv);
        myAdapter = new MainRVAdapter(this, false);
        rv.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
        rv.setAdapter(myAdapter); //Update view

    }


    //Update view to show new/edited soundboard items
    // when returning from UploadSoundboard
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check that it is the SecondActivity with an OK result
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                //update rv
                myAdapter = new MainRVAdapter(this, false);
                rv.setAdapter(myAdapter);
            }
        }
    }

    //Menu - delete button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.delete_items:
                if(myAdapter.getDelete())
                {
                    myAdapter = new MainRVAdapter(this,false);
                    rv.setAdapter(myAdapter);
                    item.setIcon(R.drawable.delete_white);
                }
                else
                {
                    myAdapter = new MainRVAdapter(this,true);
                    rv.setAdapter(myAdapter);
                    item.setIcon(R.drawable.delete_red);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
