package com.prototypes.prototype.user;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.story.Story;
import java.util.ArrayList;
import java.util.List;

public class UserProfileFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    private static final String TAG = "UserProfileFragment";
    private String userId;
    private FirebaseFirestore db;
    private ImageView imgProfile;
    private TextView tvName, tvHandle, tvFollowers;
    private RecyclerView galleryRecyclerView;
    private GalleryAdaptor galleryAdaptor;
    private FollowManager followManager;
    private Button followButton;
    private boolean isFollowing = false;
    FirebaseAuthManager authManager;
    FirestoreManager firestoreStoriesManager;

    public static UserProfileFragment newInstance(String userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
        db = FirebaseFirestore.getInstance();
        firestoreStoriesManager = new FirestoreManager(db, Story.class);
        authManager = new FirebaseAuthManager(getActivity());
        followManager = new FollowManager(db, authManager);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Use the same layout as the profile fragment but with different behavior
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        // Initialize UI elements
        imgProfile = view.findViewById(R.id.imageProfile);
        tvName = view.findViewById(R.id.tvName);
        tvHandle = view.findViewById(R.id.tvHandle);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        galleryRecyclerView = view.findViewById(R.id.gallery_recycler_view);

        // Hide maps section if it exists in the layout
        RecyclerView mapRecyclerView = view.findViewById(R.id.mapsRecyclerView);
        if (mapRecyclerView != null) {
            mapRecyclerView.setVisibility(View.GONE);
        }

        followButton = view.findViewById(R.id.btnFollow);
        followButton.setOnClickListener(v -> toggleFollowStatus());

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirestoreManager<User> firestoreManager = new FirestoreManager<>(db, User.class);

        firestoreManager.readDocument("Users", userId, new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                // Update UI with user data
                tvName.setText(user.getName());
                tvHandle.setText(user.getUsername());

                List<String> followersList = user.getFollowers();
                if (followersList == null) {
                    tvFollowers.setText("0 followers");
                } else {
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }

                // Load profile image
                Glide.with(UserProfileFragment.this)
                        .load(user.getProfile())
                        .into(imgProfile);

                // Load user's media
                loadUserMedia();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load user data: " + e.getMessage());
            }
        });
    }

    private void loadUserMedia() {
        User.getStories(getActivity(), firestoreStoriesManager, new User.UserCallback<Story>() {
            @Override
            public void onMapsLoaded(ArrayList<Story> customMaps) {
                if (customMaps.isEmpty()) {
                    galleryRecyclerView.setVisibility(View.GONE);
                    TextView tvNoPhotos = getView().findViewById(R.id.tvNoPhotos);
                    tvNoPhotos.setVisibility(View.VISIBLE);
                } else {
                    galleryRecyclerView.setVisibility(View.VISIBLE);
                    TextView tvNoPhotos = getView().findViewById(R.id.tvNoPhotos);
                    tvNoPhotos.setVisibility(View.GONE);

                    galleryAdaptor = new GalleryAdaptor(getActivity(), customMaps);
                    galleryRecyclerView.setAdapter(galleryAdaptor);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load user media: " + e.getMessage());
            }
        });

        // Check if current user is following this user
        String currentUserId = User.getUid(getActivity());
        followManager.checkFollowingStatus(currentUserId, userId, new FollowManager.FollowStatusCallback() {
            @Override
            public void onResult(boolean isFollowing) {
                UserProfileFragment.this.isFollowing = isFollowing;
                updateFollowButtonUI();
            }
        });
    }

    private void toggleFollowStatus() {
        followManager.toggleFollowStatus(userId, new FollowManager.FollowCallback() {
            @Override
            public void onSuccess(boolean isFollowing) {
                UserProfileFragment.this.isFollowing = isFollowing;
                updateFollowButtonUI();
                loadUserData(); // Refresh follower count
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to toggle follow status: " + e.getMessage());
            }
        });
    }

    private void updateFollowButtonUI() {
        followButton.setText(isFollowing ? "Following" : "Follow");

        // Change button style based on follow status
        if (isFollowing) {
            // Set button background to white when following
            followButton.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        } else {
            // Reset to default color
            followButton.getBackground().clearColorFilter();
        }
    }

}
