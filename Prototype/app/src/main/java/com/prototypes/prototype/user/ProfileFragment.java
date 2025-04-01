package com.prototypes.prototype.user;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.settings.SettingsActivity;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.signup.SignUpActivity;
import com.prototypes.prototype.story.Story;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
//    DocumentSnapshot document;
    String name, username, profile;
    List<String> followersList, stories;
    ImageView imgProfile;
    TextView tvName, tvHandle, tvFollowers, tvNoPhotos;
    RecyclerView galleryRecyclerView, mapRecyclerView;
    GalleryAdaptor galleryAdaptor;
    CustomMapAdaptor customMapAdaptor;
    private static final String TAG = "Profile Fragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this.getActivity());
        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);
        FirestoreManager firestoreStoriesManager = new FirestoreManager(db, Story.class);
        FirestoreManager firestoreMapManager = new FirestoreManager(db, CustomMap.class);

        // Get UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        galleryRecyclerView = view.findViewById(R.id.gallery_recycler_view);
        mapRecyclerView = view.findViewById(R.id.mapsRecyclerView);
        tvNoPhotos = view.findViewById(R.id.tvNoPhotos);

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
                getMedia(firestoreStoriesManager, firebaseAuthManager);
                // Retrieve all maps
                getMaps(firestoreMapManager, firebaseAuthManager);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreManager failed: "+ e);
            }
        });
        return view;
    }
    @Override
    public void onResume(){
        super.onResume();
        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(getActivity());
        FirestoreManager firestoreStoriesManager = new FirestoreManager(db, Story.class);
        FirestoreManager firestoreMapManager = new FirestoreManager(db, CustomMap.class);
        // Retrieve all media related to user
        getMedia(firestoreStoriesManager, firebaseAuthManager);
        // Retrieve all maps
        getMaps(firestoreMapManager, firebaseAuthManager);
    }
    public void getMedia(FirestoreManager firestoreStoriesManager, FirebaseAuthManager firebaseAuthManager){
        firestoreStoriesManager.queryDocuments("media", "userId", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<Story>() {
            @Override
            public void onEmpty(ArrayList<Story> storyList) {
                galleryRecyclerView.setVisibility(View.GONE);
                tvNoPhotos.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSuccess(ArrayList<Story> storyList) {
                galleryRecyclerView.setVisibility(View.VISIBLE);
                tvNoPhotos.setVisibility(View.GONE);
                galleryAdaptor = new GalleryAdaptor(getActivity(), storyList);
                galleryRecyclerView.setAdapter(galleryAdaptor);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreStoriesManager failed: " + e);
            }
        });

    }
    public void getMaps(FirestoreManager firestoreMapManager, FirebaseAuthManager firebaseAuthManager){
        firestoreMapManager.queryDocuments("map", "owner", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<CustomMap>() {
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
}