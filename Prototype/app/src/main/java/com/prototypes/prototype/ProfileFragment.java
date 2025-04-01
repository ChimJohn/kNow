package com.prototypes.prototype;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.ImageButton;


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
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.media.Stories;
import com.prototypes.prototype.user.GalleryAdaptor;
import com.prototypes.prototype.user.User;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
//    DocumentSnapshot document;
    String name, username, profile;
    List<String> followersList, stories;
    ImageView imgProfile;
    TextView tvName, tvHandle, tvFollowers;
    RecyclerView galleryRecyclerView, mapRecyclerView;
    GalleryAdaptor galleryAdaptor;
    CustomMapAdaptor customMapAdaptor;
    private static final String TAG = "Profile Fragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d(TAG, "User page.");

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this.getActivity());
        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);
        FirestoreManager firestoreStoriesManager = new FirestoreManager(db, Stories.class);
        FirestoreManager firestoreMapManager = new FirestoreManager(db, CustomMap.class);

        // Get UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        galleryRecyclerView = view.findViewById(R.id.gallery_recycler_view);
        mapRecyclerView = view.findViewById(R.id.mapsRecyclerView);

        // Menu button
        ImageButton btnMenu = view.findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (getActivity() != null) {
                Log.d(TAG, "Menu button clicked.");
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            } else {
                Log.e(TAG, "Activity is null, cannot start SettingsActivity");
            }
        });


        // Get user details
        firestoreManager.readDocument("Users", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                name = user.getName();
                username = user.getUsername();
                profile = user.getProfile();
                followersList = user.getFollowers();
                stories = user.getStories();

                // Populate UI Elements
                if (followersList == null){
                    Log.d(TAG, "User has no followers");
                    tvFollowers.setText("0 followers");

                }else{
                    Log.d(TAG, "Number of followers: " + Integer.toString(followersList.size()));
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }
                Glide.with(ProfileFragment.this)
                        .load(profile)
                        .into(imgProfile); //TODO: add buffering img
                tvName.setText(name);
                tvHandle.setText(username);

                // Retrieve all media related to user
                firestoreStoriesManager.queryDocuments("Media", "creator", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<Stories>() {
                    @Override
                    public void onEmpty(ArrayList<Stories> storyList) {
                        galleryRecyclerView.setVisibility(View.GONE);
                        TextView tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
                        tvNoPhotos.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onSuccess(ArrayList<Stories> storyList) {
                        galleryRecyclerView.setVisibility(View.VISIBLE);
                        TextView tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
                        tvNoPhotos.setVisibility(View.GONE);
                        galleryAdaptor = new GalleryAdaptor(getActivity(), storyList);
                        galleryRecyclerView.setAdapter(galleryAdaptor);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "firestoreStoriesManager failed: " + e);
                    }
                });

                // Retrieve all maps
                firestoreMapManager.queryDocuments("Map", "owner", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<CustomMap>() {
                    @Override
                    public void onEmpty(ArrayList<CustomMap> customMaps) {
                        Log.d(TAG, "Number of Custom Maps: 0");
                        customMapAdaptor = new CustomMapAdaptor(getActivity(), customMaps);
                        mapRecyclerView.setAdapter(customMapAdaptor);
                        // Create "Add" map element
                        CustomMap addMap = new CustomMap("Add", null, null);
                        customMapAdaptor.addItemToTop(addMap);
                    }
                    @Override
                    public void onSuccess(ArrayList<CustomMap> customMaps) {
                        Log.d(TAG, "Number of Custom Maps: "+ customMaps.size());
                        customMapAdaptor = new CustomMapAdaptor(getActivity(), customMaps);
                        mapRecyclerView.setAdapter(customMapAdaptor);

                        // Create "Add" map element
                        CustomMap addMap = new CustomMap("Add", null, null);
                        customMapAdaptor.addItemToTop(addMap);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "firestoreMapManager failed: "+ e);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreManager failed: "+ e);
            }
        });
        return view;
    }
}