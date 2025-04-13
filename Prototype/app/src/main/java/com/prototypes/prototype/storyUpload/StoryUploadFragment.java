package com.prototypes.prototype.storyUpload;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.CurrentLocationViewModel;
import com.prototypes.prototype.MediaViewModel;
import com.prototypes.prototype.R;
import com.prototypes.prototype.classes.Story;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.explorePage.ExploreFragment;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.user.User;

import java.util.ArrayList;

public class StoryUploadFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuthManager firebaseAuthManager;
    FirestoreManager firestoreStoriesManager, firestoreMapManager;
    private static final String ARG_MEDIA_URI = "mediaUri";
    ImageButton btnExit;
    RecyclerView selectMapRecyclerView;
    private EditText captionEditText;
    private ChipGroup categoryChipGroup;
    private MediaViewModel mediaViewModel;
    private static final String TAG = "Story Upload Fragment";
    double lat, lng;
    private SelectMapAdaptor selectMapAdaptor;

    public static StoryUploadFragment newInstance(Uri mediaUri) {
        StoryUploadFragment fragment = new StoryUploadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_URI, mediaUri.toString());
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_story_upload, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView = view.findViewById(R.id.imageView);
        captionEditText = view.findViewById(R.id.captionEditText);
        categoryChipGroup = view.findViewById(R.id.chipGroup);
        Button saveButton = view.findViewById(R.id.saveButton);
        btnExit = view.findViewById(R.id.btnExit);
        selectMapRecyclerView = view.findViewById(R.id.selectMapRecyclerView);
        selectMapRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        btnExit.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        firebaseAuthManager = new FirebaseAuthManager(requireActivity());
        firestoreMapManager = new FirestoreManager(db, CustomMap.class);
        firestoreStoriesManager = new FirestoreManager(db, Story.class);

        mediaViewModel = new ViewModelProvider(requireActivity()).get(MediaViewModel.class);
        String userId = firebaseAuthManager.getCurrentUser().getUid();
        String mediaUriString = getArguments().getString(ARG_MEDIA_URI, "");
        Uri mediaUri = Uri.parse(mediaUriString);
        mediaViewModel.uploadMediaAndThumbnailInBackground(mediaUri);

        //If video
        if (mediaUri.toString().endsWith(".mp4")) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), mediaUri);
                Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC); //get first frame to use as thumbnail
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
                retriever.release();
            } catch (Exception e) {
                Log.e("VideoThumbnailError", "Error extracting video thumbnail: " + e.getMessage());
            }
        } else {
            Glide.with(requireContext())
                    .load(mediaUri)
                    .into(imageView);
        }

        //get the exact location when user starts taking photo/video
        CurrentLocationViewModel currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);
        Location lastKnownLocation = currentLocationViewModel.getLastKnownLocation();
        lat = lastKnownLocation.getLatitude();
        lng = lastKnownLocation.getLongitude();

        User.getMaps(getActivity(), firestoreMapManager, new User.UserCallback<CustomMap>() {
            @Override
            public void onSuccess(ArrayList<CustomMap> customMaps) {
                if (!customMaps.isEmpty()) {
                    selectMapAdaptor = new SelectMapAdaptor(getActivity(), customMaps);
                    selectMapRecyclerView.setAdapter(selectMapAdaptor);
                } else {
                    selectMapAdaptor = new SelectMapAdaptor(getActivity(), new ArrayList<>());
                }
                saveButton.setOnClickListener(v -> {
                    String caption = captionEditText.getText().toString();
                    String mediaType = Story.checkMediaType(mediaUri.toString());
                    mediaViewModel.saveMediaToFirebaseStorage(userId, caption, getSelectedCategory(), lat, lng, mediaType, selectMapAdaptor.getSelectedCustomMapsId());
                    ExploreFragment exploreFragment = new ExploreFragment();
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, exploreFragment)
                            .addToBackStack(null)
                            .commit();
                    Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
                });

            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreMapManager failed: "+ e);
            }
        });
    }

    private String getSelectedCategory() {
        int selectedId = categoryChipGroup.getCheckedChipId(); // Get selected chip ID
        if (selectedId == R.id.chipNone) {
            return "None";
        } else if (selectedId == R.id.chipFood) {
            return "Food";
        } else if (selectedId == R.id.chipAttraction) {
            return "Attractions";
        }
        return "None";
    }

}
