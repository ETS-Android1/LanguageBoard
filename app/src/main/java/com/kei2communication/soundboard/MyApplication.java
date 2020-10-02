package com.kei2communication.soundboard;

import android.app.Application;

import java.util.ArrayList;

public class MyApplication extends Application {

    private ArrayList<Soundboard> soundboards;
    private ArrayList<String> presetNames;
    private ArrayList<String> userSoundboardNames;
    private boolean tutorial;

    public MyApplication(){
        presetNames = new ArrayList<>();
        userSoundboardNames = new ArrayList<>();
        soundboards = new ArrayList<>();
        tutorial = false;
    }

    public ArrayList<Soundboard> getSoundboards(){
        return soundboards;
    }
    public void addPresetName(String name){
        presetNames.add(name);
    }
    public ArrayList<String> getPresetNames(){return presetNames;}
    public void addUserSoundboardName(String name){
        userSoundboardNames.add(name);
    }
    public ArrayList<String> getUserSoundboardNames(){return userSoundboardNames;}
    public boolean inTutorial(){return tutorial;}
    public void setTutorial(Boolean tutorial) {this.tutorial = tutorial;}

    public int findPosition(String name){
        name = name.toLowerCase();
        for(int i = 0; i < soundboards.size(); i++){
            if(soundboards.get(i).getName().toLowerCase().equals(name)){
                return i;
            }
        }
        return -1;
    }
}
