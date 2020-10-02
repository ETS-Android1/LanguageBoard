package com.kei2communication.soundboard;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.RecyclerView;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class SoundboardRVAdapter extends RecyclerView.Adapter<SoundboardRVAdapter.MyViewHolder>{

    private Context mContext;
    private List<SBCard> mData;
    private boolean showDelete;
    private String soundboardName;
    private TourGuide mTourGuideHandler;
    private MyApplication mApp;
    RelativeLayout[] card_back_arr, card_front_arr;

    public SoundboardRVAdapter(Context mContext, boolean showDelete, String soundboardName){
        this.mContext = mContext;
        mApp = (MyApplication) mContext.getApplicationContext();
        int pos = mApp.findPosition(soundboardName);
        this.mData = mApp.getSoundboards().get(pos).getSoundboardCards();
        this.showDelete = showDelete;
        this.soundboardName = soundboardName;
        card_back_arr = new RelativeLayout[mData.size()];
        card_front_arr = new RelativeLayout[mData.size()];
    }

    public boolean getDelete(){return showDelete;}


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.soundboard_card, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.name_f.setText(mData.get(position).getName());
        holder.name_b.setText(mData.get(position).getName());
        Picasso.with(mContext).load(mData.get(position).getImage()).fit().into(holder.image);
        card_back_arr[position] = holder.card_back;
        card_front_arr[position] = holder.card_front;

        //if position == 0 and first time... show tourguide pointer
        if(position == 0 && mApp.inTutorial()){
            mTourGuideHandler = TourGuide.init((Activity)mContext).with(TourGuide.Technique.CLICK)
                    .setPointer(new Pointer())
                    .setToolTip(new ToolTip().setDescription("Tap and hold to flip the item card"));

            mTourGuideHandler.setOverlay(new Overlay());
            mTourGuideHandler.playOn(holder.card_front);
        }

        //Show Delete button on card
        if(showDelete)
            holder.delete.setVisibility(VISIBLE);

        //Flip from front to back
        holder.card_front.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                flipFrontToBack(holder, position);
                return true;
            }
        });

        //Flip from back to front
        holder.card_back.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                flipBackToFront(holder, position);
                return false;
            }
        });

        //Back button - flip card back to front
        holder.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipBackToFront(holder, position);
            }
        });

        //Listener for card touch - play audio if exists
        holder.card_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean fileExists = mData.get(position).playAudio(mContext, mData.get(position).getOutputFile());

                if(position == 0 && mApp.inTutorial() && fileExists){
                    mTourGuideHandler.cleanUp();
                }

            }
        });

        //Play Button press - play audio
        holder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData.get(position).playAudio(mContext, mData.get(position).getOutputFile());
            }
        });

        //Record button - start recording
        holder.record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData.get(position).recordAudio(mContext);
                holder.stop.setVisibility(VISIBLE); //show stop button
                holder.record.setVisibility(GONE); //hide record button
                holder.record_text.setText("Stop Recording");

                //tutorial
                if(position == 0 && mApp.inTutorial()){
                    mTourGuideHandler.cleanUp();
                }
            }
        });

        //Stop button - stop recording audio, save file
        holder.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mData.get(position).stopAudio(mContext);
                holder.stop.setVisibility(GONE); //hide stop button
                holder.record.setVisibility(VISIBLE); //show record button
                holder.record_text.setText("Start Recording");
            }
        });

        //Delete button
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Initiate database and authorization
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                //delete entry from database
                db.collection("users").document(mAuth.getUid())
                        .collection("soundboards").document(soundboardName.toLowerCase())
                        .collection("soundboard_cards").document(mData.get(position).getName().toLowerCase())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.w("TESTING", "Deleted Document");
                        //Remove item from list after it is deleted from database
                        mData.remove(position);
                        notifyItemRemoved(position);
                    }
                });

            }
        });

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView name_f, name_b, record_text;
        ImageView image;
        RelativeLayout card_front, card_back, delete;
        ImageView record, play, stop, back;
        Button deleteButton;

        public MyViewHolder(View itemView){
            super(itemView);
            card_front = itemView.findViewById(R.id.card_front);
            card_back = itemView.findViewById(R.id.card_back);
            delete = itemView.findViewById(R.id.delete);
            name_f = itemView.findViewById(R.id.name_front);
            name_b =  itemView.findViewById(R.id.name_back);
            record_text = itemView.findViewById(R.id.record_helper_text);
            image = itemView.findViewById(R.id.image_front);
            record = itemView.findViewById(R.id.record);
            play =  itemView.findViewById(R.id.play);
            stop = itemView.findViewById(R.id.stop);
            back = itemView.findViewById(R.id.back);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

    }


    //Flip card from back to front
    public void flipBackToFront(final MyViewHolder holder, int position){
        //Flip animation
        FlipAnimator.flipView(mContext, card_back_arr[position], card_front_arr[position], mData.get(position).isFront(), position);
        //show the front card again
        mData.get(position).setFront();
        holder.card_front.setVisibility(VISIBLE);

        //if position == 0 and first time... show tourguide pointer
        if(position == 0 && mApp.inTutorial()){
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTourGuideHandler.setToolTip(new ToolTip().setDescription("Tap the item card to play your recording!"));
                    mTourGuideHandler.playOn(holder.card_front);
                }
            }, 1000);
        }
    }


    //Flip card from front to back
    public void flipFrontToBack(final MyViewHolder holder, final int position){
        //Flip animation
        FlipAnimator.flipView(mContext, card_back_arr[position], card_front_arr[position], mData.get(position).isFront(), position);
        //sleep for one second (wait for animation) - then hide the front card
        //front card must be hidden to interact with the back card
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                holder.card_front.setVisibility(GONE);

                if(position == 0 && mApp.inTutorial()){
                    mTourGuideHandler.cleanUp();
                    mTourGuideHandler.setToolTip(new ToolTip().setDescription("Try recording audio for your language board item"));
                    mTourGuideHandler.playOn(holder.record);
                }

            }
        }, 1000);
        mData.get(position).setFront();
    }





}
