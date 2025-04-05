package com.prototypes.prototype;

import android.content.Context;
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

import com.google.android.material.chip.ChipGroup;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.story.Story;
import com.prototypes.prototype.upload.SelectMapAdaptor;
import com.prototypes.prototype.user.User;

import java.util.ArrayList;

public class StoryUploadFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String ARG_MEDIA_URI = "mediaUri";
    private ImageView imageView;
    ImageButton btnExit;
    RecyclerView selectMapRecyclerView;
    private EditText captionEditText;
    private Button saveButton;
    private ChipGroup categoryChipGroup;
    private FirebaseAuthManager firebaseAuthManager;
    FirestoreManager firestoreMapManager;
    private MediaViewModel mediaViewModel;
    private static final String TAG = "Story Upload Fragment";


    double lat, lng;
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

        CurrentLocationViewModel currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);

        mediaViewModel = new ViewModelProvider(requireActivity()).get(MediaViewModel.class);
        firebaseAuthManager = new FirebaseAuthManager(requireActivity());
        firestoreMapManager = new FirestoreManager(db, CustomMap.class);

        imageView = view.findViewById(R.id.imageView);
        captionEditText = view.findViewById(R.id.captionEditText);
        categoryChipGroup = view.findViewById(R.id.chipGroup);
        saveButton = view.findViewById(R.id.saveButton);
        btnExit = view.findViewById(R.id.btnExit);
        selectMapRecyclerView = view.findViewById(R.id.selectMapRecyclerView);
        selectMapRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        String userId = firebaseAuthManager.getCurrentUser().getUid();
        String mediaUriString = getArguments().
                getString(ARG_MEDIA_URI, "");
        Uri mediaUri = Uri.parse(mediaUriString);

        // Display all custom maps in recycler view
        getMaps();

        mediaViewModel.uploadMediaAndThumbnailInBackground(mediaUri);
        if (mediaUri.toString().endsWith(".mp4")) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), mediaUri);
                Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
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

        Location lastKnownLocation = currentLocationViewModel.getLastKnownLocation();
        lat = lastKnownLocation.getLatitude();
        lng = lastKnownLocation.getLongitude();

        saveButton.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString();
            String mediaType;
            if (mediaUri.toString().endsWith(".mp4")) {
                mediaType = "video";
            }
            else{
                mediaType = "photo";
            }
            mediaViewModel.saveMediaToFirebaseStorage(userId, caption, getSelectedCategory(), lat, lng, mediaType);
            ExploreFragment exploreFragment = new ExploreFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, exploreFragment) // Replace with the correct container ID
                    .addToBackStack(null) // Optional, allows going back to previous fragment
                    .commit();
            Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
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
        return "None"; // Default category if none is selected
    }

    public void getMaps(){
        User.getMaps(getActivity(), new User.UserCallback<CustomMap>() {
            @Override
            public void onMapsLoaded(ArrayList<CustomMap> customMaps) {
                if (customMaps.size() > 0) {
                    SelectMapAdaptor selectMapAdaptor = new SelectMapAdaptor(getActivity(), customMaps);
                    selectMapRecyclerView.setAdapter(selectMapAdaptor);
                } else {
                    Log.d(TAG, "No Maps");
                }
            }
            @Override
            public void onError(Exception e) {
                Log.d(TAG, "firestoreMapManager failed: "+ e);
            }
        });
    }
}
