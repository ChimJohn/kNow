package com.prototypes.prototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FieldValue;
import java.util.Date;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.prototypes.prototype.firebase.FirebaseAuthManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoryUploadFragment extends Fragment {
    private ImageView imageView;
    private EditText captionEditText;
    private Button saveButton;
    private ChipGroup categoryChipGroup;
    private static final String ARG_PHOTO_URI = "photoUri";
    private CurrentLocationViewModel currentLocationViewModel;
    private FirebaseAuthManager firebaseAuthManager;
    private Uri photoUri;
    double lat, lng;

    public static StoryUploadFragment newInstance(Uri photoUri) {
        StoryUploadFragment fragment = new StoryUploadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_URI, photoUri.toString()); // Convert Uri to String
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_story_upload, container, false);
        currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);
        firebaseAuthManager = new FirebaseAuthManager(requireActivity());

        imageView = rootView.findViewById(R.id.imageView);
        captionEditText = rootView.findViewById(R.id.captionEditText);
        categoryChipGroup = rootView.findViewById(R.id.chipGroup); // Get the RadioGroup
        saveButton = rootView.findViewById(R.id.saveButton);

        // Get the photo URI from arguments
        String photoUriString = getArguments().getString("photoUri", "");
        Uri photoUri = Uri.parse(photoUriString);

        // Load the image into the ImageView
        Glide.with(requireContext()) // Using Glide to load the image
                .load(photoUri)
                .into(imageView);

        // Handle save button click
        saveButton.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString();
            uploadImageToFirebaseStorage(photoUri, caption);
            ExploreFragment exploreFragment = new ExploreFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, exploreFragment) // Replace with the correct container ID
                    .addToBackStack(null) // Optional, allows going back to previous fragment
                    .commit();
            Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
        });

        Location lastKnownLocation = currentLocationViewModel.getLastKnownLocation();
        Log.d("LocationUpdate", "Last known location: " + lastKnownLocation);
        lat = lastKnownLocation.getLatitude();
        lng = lastKnownLocation.getLongitude();
        return rootView;
    }

    private void uploadImageToFirebaseStorage(Uri uri, String caption) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference("media/" + UUID.randomUUID().toString());

        storageReference.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        String selectedCategory = getSelectedCategory();
                                        String url = downloadUri.toString();
                                        saveUrlToFirestore(firebaseAuthManager.getCurrentUser().getUid(), url, caption, selectedCategory, lat, lng);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("UploadFragment", "Upload failed: " + e.getMessage());
                    }
                });
    }

    //Image URL to be saved in Firestore
    private void saveUrlToFirestore(String userId, String url, String caption, String selectedCategory, Double lat, Double lng) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("user",userId);
        data.put("imageUrl", url);
        data.put("caption", caption);
        data.put("category", selectedCategory);
        data.put("latitude", lat);
        data.put("longitude", lng);
        data.put("timestamp", FieldValue.serverTimestamp()); // Firestore server timestamp

        db.collection("media").document()
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.d("UploadFragment", "Image URL saved to Firestore");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("UploadFragment", "Failed to save URL to Firestore: " + e.getMessage());
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



}
