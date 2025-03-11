package com.prototypes.prototype;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.user.User;

import java.util.List;

public class ProfileFragment extends Fragment {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
//    DocumentSnapshot document;
    String name, username, profile;
    List<String> followersList;
    ImageView imgProfile;
    TextView tvName, tvHandle, tvFollowers;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d("Debug", "User page.");

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this.getActivity());
        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);

        // Get UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);

        // Get user details
//        loadUserDetails();
        firestoreManager.readDocument("Users", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                name = user.getName();
                username = user.getUsername();
                profile = user.getProfile();
                followersList = user.getFollowers();

                if (followersList == null){
                    Log.d("Debug", "No Users");
                    tvFollowers.setText("0 followers");

                }else{
                    Log.d("Debug", "Number of followers: " + Integer.toString(followersList.size()));
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }
                // Populate UI Elements
                Glide.with(ProfileFragment.this)
                        .load(profile)
                        .into(imgProfile); //TODO: add buffering img
                tvName.setText(name);
                tvHandle.setText(username);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("Debug", "get failed with ", e);
            }
        });
        // Load user gallery
        return view;
    }
}