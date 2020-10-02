package com.kei2communication.soundboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SoundboardActivity extends AppCompatActivity {

    private static final int UPLOAD_ITEM_REQUEST_CODE = 0;
    private String sbDisplayName;
    private int position;
    private MyApplication mApp;
    private RecyclerView rv;
    private ImageView backButton;
    private SoundboardRVAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundboard);

        mApp = (MyApplication) getApplication();
        backButton = findViewById(R.id.back);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }


        //Get data from intent
        Intent intent = getIntent();
        position = intent.getIntExtra("soundboardPosition", 0);
        sbDisplayName = intent.getStringExtra("display name");
        this.setTitle(sbDisplayName);

        //Setup recyclerview with soundboardCards from intent
        rv = findViewById(R.id.rv);
        myAdapter = new SoundboardRVAdapter(this, false, sbDisplayName);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(myAdapter);

        //Show add button only on the user's soundboards
        FloatingActionButton addSoundboard = findViewById(R.id.add_button);
        if(mApp.getPresetNames().contains(sbDisplayName))
        {
            addSoundboard.setVisibility(View.GONE);
        }
        else{
            //Add button listener
            addSoundboard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), UploadItem.class);
                    intent.putExtra("soundboardPosition", position);
                    intent.putExtra("category", sbDisplayName);
                    startActivityForResult(intent, UPLOAD_ITEM_REQUEST_CODE);
                }
            });
        }

        //Back button listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    //Menu - delete button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!mApp.getPresetNames().contains(sbDisplayName)){
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu, menu);
            return true;
        }
        else
            return false;
    }

    //Handle menu actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.delete_items:
                if(myAdapter.getDelete())
                {
                    myAdapter = new SoundboardRVAdapter(getApplicationContext(),false, sbDisplayName);
                    rv.setAdapter(myAdapter);
                    item.setIcon(R.drawable.delete_white);
                }
                else
                {
                    myAdapter = new SoundboardRVAdapter(getApplicationContext(),true, sbDisplayName);
                    rv.setAdapter(myAdapter);
                    item.setIcon(R.drawable.delete_red);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Update view to show new/edited soundboard items
    // when returning from UploadItem
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check that it is the SecondActivity with an OK result
        if (requestCode == UPLOAD_ITEM_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //update rv
                myAdapter = new SoundboardRVAdapter(getApplicationContext(), false, sbDisplayName);
                rv.setAdapter(myAdapter);
            }
        }
    }

    @Override
    public void onBackPressed(){
        mApp.setTutorial(false);
        super.onBackPressed();
    }

}
