package com.prototypes.prototype.user;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.custommap.OtherCustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.story.Story;
import java.util.ArrayList;
import java.util.List;

public class UserProfileFragment extends Fragment {
    private static final String ARG_USER_ID = "user_id";
    private static final String TAG = "UserProfileFragment";
    private String userId, profile;
    private FirebaseFirestore db;
    private ImageView imgProfile;
    private TextView tvName, tvHandle, tvFollowers;
    private RecyclerView mapRecyclerView;
    private GridView galleryGridView;
    private GalleryAdaptor galleryAdaptor;
    private FollowManager followManager;
    private Button followButton;
    private boolean isFollowing = false;
    LinearLayout linearLayoutMaps;
    OtherCustomMapAdaptor otherCustomMapAdaptor;
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
        galleryGridView = view.findViewById(R.id.gallery_recycler_view);
        mapRecyclerView = view.findViewById(R.id.mapsRecyclerView);
        mapRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        linearLayoutMaps = view.findViewById(R.id.linearLayoutMaps);

        followButton = view.findViewById(R.id.btnFollow);
        followButton.setOnClickListener(v -> toggleFollowStatus());

        // Load user data
        loadUserData();
        // Load user maps
        loadUserMaps();

        return view;
    }

    private void loadUserData() {
        FirestoreManager<User> firestoreManager = new FirestoreManager<>(db, User.class);

        firestoreManager.readDocument("Users", userId, new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                // Update UI with user data
                tvName.setText(user.getName());
                tvHandle.setText("@"+user.getUsername());
                profile = user.getProfile();

                List<String> followersList = user.getFollowers();
                if (followersList == null) {
                    tvFollowers.setText("0 followers");
                } else {
                    tvFollowers.setText(String.format("%d followers", followersList.size()));
                }

                // Load profile image
                if (profile == null){
                    Glide.with(getActivity())
                            .load(R.drawable.default_profile)
                            .into(imgProfile); //TODO: add buffering img
                }else{
                    Glide.with(getActivity())
                            .load(profile)
                            .into(imgProfile); //TODO: add buffering img
                }

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
        FirestoreManager<Story> firestoreStoriesManager = new FirestoreManager<>(db, Story.class);

        firestoreStoriesManager.queryDocuments("media", "userId", userId,
                new FirestoreManager.FirestoreQueryCallback<Story>() {
                    @Override
                    public void onEmpty(ArrayList<Story> storyList) {
                        galleryGridView.setVisibility(View.GONE);
                        TextView tvNoPhotos = getView().findViewById(R.id.tvNoPhotos);
                        tvNoPhotos.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onSuccess(ArrayList<Story> storyList) {
                        galleryGridView.setVisibility(View.VISIBLE);
                        TextView tvNoPhotos = getView().findViewById(R.id.tvNoPhotos);
                        tvNoPhotos.setVisibility(View.GONE);

                        galleryAdaptor = new GalleryAdaptor(getActivity(), storyList);
                        galleryGridView.setAdapter(galleryAdaptor);
                        setGridViewHeightBasedOnChildren(galleryGridView, 3);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to load user media: " + e.getMessage());
                    }
                });
        // Check if current user is following this user
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        followManager.checkFollowingStatus(currentUserId, userId, new FollowManager.FollowStatusCallback() {
            @Override
            public void onResult(boolean isFollowing) {
                UserProfileFragment.this.isFollowing = isFollowing;
                updateFollowButtonUI();
            }
        });
    }

    private void loadUserMaps() {
        FirestoreManager<CustomMap> firestoreStoriesManager = new FirestoreManager<>(db, CustomMap.class);
        firestoreStoriesManager.queryDocuments("map", "owner", userId, new FirestoreManager.FirestoreQueryCallback<CustomMap>() {
            @Override
            public void onEmpty(ArrayList<CustomMap> results) {
                Log.d(TAG, "Empty maps list");
                linearLayoutMaps.setVisibility(View.GONE);
            }

            @Override
            public void onSuccess(ArrayList<CustomMap> results) {
                otherCustomMapAdaptor = new OtherCustomMapAdaptor(getActivity(), results);
                mapRecyclerView.setAdapter(otherCustomMapAdaptor);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "Error: "+e.getMessage());
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
    public static void setGridViewHeightBasedOnChildren(GridView gridView, int numColumns) {
        ListAdapter adapter = gridView.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int items = adapter.getCount();
        int rows = (int) Math.ceil((double) items / numColumns);

        for (int i = 0; i < rows; i++) {
            View listItem = adapter.getView(i, null, gridView);
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
