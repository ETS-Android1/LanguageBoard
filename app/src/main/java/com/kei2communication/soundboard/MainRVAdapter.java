package com.kei2communication.soundboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import java.util.List;
import androidx.recyclerview.widget.RecyclerView;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import static android.view.View.VISIBLE;

public class MainRVAdapter extends RecyclerView.Adapter<MainRVAdapter.MyViewHolder> {

    private Context mContext;
    private List<Soundboard> mData;
    private  TourGuide mTourGuideHandler;
    private MyApplication mApp;
    private boolean showDelete;


    public MainRVAdapter(Context mContext, boolean showDelete) {
        this.mContext = mContext;
        mApp = (MyApplication)mContext.getApplicationContext();
        mData = mApp.getSoundboards();
        this.showDelete = showDelete;
    }

    public boolean getDelete(){return showDelete;}

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.main_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        //Set card text and load image
        holder.name.setText(mData.get(position).getName());
        Picasso.with(mContext).load(mData.get(position).getImage()).fit().into(holder.image);

        //if position == 0 and tutorial ... show tourguide pointer
        if(position == 0 && mApp.inTutorial()){
            mTourGuideHandler = TourGuide.init((Activity)mContext).with(TourGuide.Technique.CLICK)
                    .setPointer(new Pointer())
                    .setToolTip(new ToolTip().setTitle("Welcome!").setDescription("Tap to open the language board"));
            mTourGuideHandler.setOverlay(new Overlay());
            mTourGuideHandler.playOn(holder.card);
        }

        //show (or hide) delete button on cards
        if(showDelete){
            holder.delete.setVisibility(VISIBLE);
            if(mApp.getPresetNames().contains(mData.get(position).getName())){
                holder.deleteButton.setVisibility(View.GONE);
            }
        }

        //Card press opens new SoundboardActivity
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext,SoundboardActivity.class);
                intent.putExtra("soundboardPosition", position);
                intent.putExtra("display name", mData.get(position).getName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);

                if(position == 0 && mApp.inTutorial()){
                    mTourGuideHandler.cleanUp();
                }
            }
        });


        //Delete button
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Prompt the user to confirm deleting their soundboard
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Confirm Delete");
                builder.setMessage("Are you sure you want to delete this language board. It cannot be recovered.");
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete board from database
                        deleteSoundboard(mData.get(position).getName().toLowerCase());
                        //Remove item from list after it is deleted from database
                        mData.remove(position);
                        notifyItemRemoved(position);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { }});
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ImageView image;
        RelativeLayout card, delete;
        Button deleteButton;


        public MyViewHolder(View itemView){
            super(itemView);
            name = itemView.findViewById(R.id.name);
            image = itemView.findViewById(R.id.image);
            card =  itemView.findViewById(R.id.card);
            delete =  itemView.findViewById(R.id.delete);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }

    //Delete soundboard document from firebase
    public void deleteSoundboard(final String name){
        //Initiate database and authorization
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        //delete entry from database
        db.collection("users").document(mAuth.getUid())
                .collection("soundboards").document(name)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.w("TESTING", "Deleted Document");
                    }
                });

        db.collection("users").document(mAuth.getUid())
                .collection("soundboards").document(name)
                .collection("soundboard_cards").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List <DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                for(int i = 0; i < list.size(); i++){
                    db.collection("users")
                            .document(mAuth.getUid())
                            .collection("soundboards")
                            .document(name)
                            .collection("soundboard_cards")
                            .document(list.get(i).get("name").toString().toLowerCase())
                            .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    });
                }

            }
        });


    }


}
