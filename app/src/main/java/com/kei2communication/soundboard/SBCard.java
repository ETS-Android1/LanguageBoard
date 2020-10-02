package com.kei2communication.soundboard;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

public class SBCard{

    private String outputFile, name, image;
    private boolean front, recording;
    private MediaRecorder myAudioRecorder;
    //static mediaplayer to prevent multiple audio files playing at once
    private static MediaPlayer mediaPlayer;

    public SBCard(String name, String image, String soundboardName){
        this.name = name;
        this.image = image;
        front = true;
        recording = false;
        //set the output file to /soundboards/{SOUNDBOARD CATEGORY}/{CARD NAME}.ogg
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/soundboards/" + soundboardName +"/" + name + ".ogg";
        //If the directory doesn't exist, make one
        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/soundboards/" + soundboardName);
        try { path.mkdirs(); }  catch(Exception E){}
    }

    public void setFront() {
        this.front = !this.front;
    }
    public boolean isFront(){ return front;}
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public String getOutputFile() {
        return outputFile;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }


    /*
     * play audio from output file
     */
    public boolean  playAudio(Context mContext, String outputFile){
        //return if the file doesn't exist
        File f = new File(outputFile);
        if(!f.exists() || f == null)
            return false;
        //If the mediaPlayer already exists and is already playing
        //stop the current mediaPlayer before initiating a new one
        if(mediaPlayer != null){
            if (mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
        //Initiate a new mediaplayer and play the file
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(outputFile);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(mContext, "Playing Audio", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { }

        return true;
    }

    /*
     * start recording audio from phone mic
     */
    public void recordAudio(Context mContext){
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);
        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
            Toast.makeText(mContext, "Recording Started", Toast.LENGTH_SHORT).show();
        } catch (Exception E) { }
    }

    /*
     * stop recording
     * save audio to output file
     */
    public void stopAudio(Context mContext){
        if(myAudioRecorder != null){
            myAudioRecorder.stop();
            myAudioRecorder.release();
            Toast.makeText(mContext, "Recording Saved", Toast.LENGTH_SHORT).show();
        }
    }

}
