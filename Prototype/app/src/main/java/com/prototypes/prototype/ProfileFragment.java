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
import androidx.recyclerview.widget.RecyclerView;

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
import com.prototypes.prototype.media.Stories;
import com.prototypes.prototype.user.GalleryAdaptor;
import com.prototypes.prototype.user.User;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
//    DocumentSnapshot document;
    String name, username, profile;
    List<String> followersList, stories;
    ImageView imgProfile;
    TextView tvName, tvHandle, tvFollowers;
    RecyclerView recyclerView;
    GalleryAdaptor galleryAdaptor;
    private static final String TAG = "Profile Fragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d(TAG, "User page.");

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this.getActivity());
        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);
        FirestoreManager firestoreStoriesManager = new FirestoreManager(db, Stories.class);


        // Get UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        recyclerView = view.findViewById(R.id.gallery_recycler_view);

        // Get user details
        firestoreManager.readDocument("Users", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                name = user.getName();
                username = user.getUsername();
                profile = user.getProfile();
                followersList = user.getFollowers();
                stories = user.getStories();

                if (followersList == null){
                    Log.d(TAG, "User has no followers");
                    tvFollowers.setText("0 followers");

                }else{
                    Log.d(TAG, "Number of followers: " + Integer.toString(followersList.size()));
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }
                // Populate UI Elements
                Glide.with(ProfileFragment.this)
                        .load(profile)
                        .into(imgProfile); //TODO: add buffering img
                tvName.setText(name);
                tvHandle.setText(username);

                // Retrieve all media related to user
                firestoreStoriesManager.queryDocuments("Media", "creator", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<Stories>() {
                    @Override
                    public void onSuccess(ArrayList<Stories> storyList) {
                        if (storyList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            TextView tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
                            tvNoPhotos.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            TextView tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
                            tvNoPhotos.setVisibility(View.GONE);

                            galleryAdaptor = new GalleryAdaptor(getActivity(), storyList);
                            recyclerView.setAdapter(galleryAdaptor);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "firestoreStoriesManager failed: ", e);
                    }
                });

            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreManager failed: ", e);
            }
        });
        return view;
    }
}