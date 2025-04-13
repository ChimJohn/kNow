package com.prototypes.prototype.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.classes.Story;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.custommap.editmaps.EditMaps;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.settings.SettingsActivity;
import com.prototypes.prototype.storyView.StoryViewFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment {

    FirebaseFirestore db ;
    FirestoreManager firestoreManager, firestoreMapManager, firestoreStoriesManager;
    String name, username, profile;
    List<String> followersList, stories;
    ImageView imgProfile;
    TextView tvName, tvHandle, tvFollowers, tvNoPhotos;
    ImageButton btnMenu;
    RecyclerView mapRecyclerView;
    GridView galleryGridView;
    Button btnEditMap, btnEditProfile;
    GalleryAdaptor galleryAdaptor;
    CustomMapAdaptor customMapAdaptor;
    private static final String TAG = "Profile Fragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        firestoreManager = new FirestoreManager(db, User.class);
        firestoreMapManager = new FirestoreManager(db, CustomMap.class);
        firestoreStoriesManager = new FirestoreManager(db, Story.class);

        // Get UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        galleryGridView = view.findViewById(R.id.gallery_recycler_view);
        mapRecyclerView = view.findViewById(R.id.mapsRecyclerView);
        tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
        btnEditMap = view.findViewById(R.id.btnEditMap);
        btnMenu = view.findViewById(R.id.btnMenu);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Menu button
        btnMenu.setOnClickListener(v -> {
            Log.d(TAG, "Menu button clicked. Launching SettingsActivity.");
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        // Edit profile button
        btnEditProfile.setOnClickListener(v -> {
            Log.d(TAG, "Edit Profile button clicked.");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,  // enter
                            R.anim.slide_out_left,  // exit
                            R.anim.slide_in_left,   // popEnter (back stack)
                            R.anim.slide_out_right  // popExit (back stack)
                    )
                    .replace(R.id.fragment_container, new EditProfileFragment()) // Replace with your real container ID
                    .addToBackStack(null)
                    .commit();
        });


        // Edit Maps
        btnEditMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditMaps.class);
                startActivity(intent);
            }
        });
        // Get user details
        getUser();

        return view;
    }
    @Override
    public void onResume(){
        super.onResume();
        // Retrieve all media related to user
        getMedia();
        // Retrieve all maps
        getMaps();
    }
    public void getUser(){
        User.getUserData(getActivity(), firestoreManager, new User.UserReadCallback<User>() {
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
                } else
                {
                    Log.d(TAG, "Number of followers: " + Integer.toString(followersList.size()));
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }
                tvName.setText(name);
                tvHandle.setText("@"+username);
                if (getView() != null && isAdded()) {
                    if (profile == null){
                        Glide.with(ProfileFragment.this)
                                .load(R.drawable.default_profile)
                                .into(imgProfile); //TODO: add buffering img
                    }
                    else {
                        Glide.with(ProfileFragment.this)
                                .load(profile)
                                .into(imgProfile); //TODO: add buffering img
                    }
                }
                // Retrieve all media related to user
                getMedia();
                // Retrieve all maps
                getMaps();
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreManager failed: "+ e);
            }
        });
    }
    public void getMedia(){
        User.getStories(getActivity(), firestoreStoriesManager, new User.UserCallback<Story>() {
            @Override
            public void onSuccess(ArrayList<Story> customMaps) {
                if (customMaps.isEmpty()){
                    galleryGridView.setVisibility(View.GONE);
                    tvNoPhotos.setVisibility(View.VISIBLE);
                }
                else {
                    galleryGridView.setVisibility(View.VISIBLE);
                    tvNoPhotos.setVisibility(View.GONE);
                    galleryAdaptor = new GalleryAdaptor(getActivity(), customMaps, story -> {
                        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(Collections.singletonList(story)));
                        transaction.addToBackStack(null);
                        transaction.commit();
                        if (getActivity() != null) {
                            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
                            if (bottomNav != null) {
                                bottomNav.setVisibility(View.GONE);
                            }
                        }
                    });
                    galleryGridView.setAdapter(galleryAdaptor);
                    setGridViewHeightBasedOnChildren(galleryGridView, 3);
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreStoriesManager failed: " + e);
            }
        });
    }
    public void getMaps(){
        User.getMaps(getActivity(), firestoreMapManager, new User.UserCallback<CustomMap>() {
            @Override
            public void onSuccess(ArrayList<CustomMap> customMaps) {
                if (customMaps.isEmpty()){
                    customMapAdaptor = new CustomMapAdaptor(getActivity(), customMaps, getActivity());
                    mapRecyclerView.setAdapter(customMapAdaptor);
                    // Create "Add" map element
                    CustomMap addMap = new CustomMap("Add", null, null, "");
                    customMapAdaptor.addItemToTop(addMap);
                }
                else {
                    customMapAdaptor = new CustomMapAdaptor(getActivity(), customMaps, getActivity());
                    mapRecyclerView.setAdapter(customMapAdaptor);
                    // Create "Add" map element
                    CustomMap addMap = new CustomMap("Add", null, null, "");
                    customMapAdaptor.addItemToTop(addMap);
                }
            }
            @Override
            public void onFailure(Exception e) {
                    Log.d(TAG, "firestoreManager failed: "+ e);
            }
        });
    }

    public static void setGridViewHeightBasedOnChildren(GridView gridView, int numColumns) {
        if (gridView == null){
            return;
        }
        ListAdapter adapter = gridView.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int items = adapter.getCount();
        int rows = (int) Math.ceil((double) items / numColumns);

        for (int i = 0; i < rows; i++) {
            View listItem = adapter.getView(i, null, gridView);
            if (listItem == null){
                return;
            }
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(gridView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight + (gridView.getVerticalSpacing() * (rows - 1));
        gridView.setLayoutParams(params);
        gridView.requestLayout();
    }
}