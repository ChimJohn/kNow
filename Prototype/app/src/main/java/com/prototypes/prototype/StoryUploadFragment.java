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

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

import android.graphics.Bitmap;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

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
        StorageReference fullImageRef = storage.getReference("Media/" + UUID.randomUUID().toString());
        StorageReference thumbnailRef = storage.getReference("Thumbnails/" + UUID.randomUUID().toString());

        // Step 1: Compress and Rotate Image (if necessary)
        try {
            // Get the original bitmap from the URI
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);

            // Handle EXIF orientation
            originalBitmap = rotateImageIfRequired(originalBitmap, uri);

            // Step 2: Create a copy of the original image without altering resolution
            Bitmap fullImageBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);  // Create a mutable copy of the original image

            // Step 3: Compress and convert the full image for storage
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            fullImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);  // Compress at 50% quality
            byte[] fullImageData = baos.toByteArray();

            // Step 4: Create a smaller thumbnail based on the aspect ratio
            float aspectRatio = (float) originalBitmap.getWidth() / (float) originalBitmap.getHeight();
            int newWidth = 250;
            int newHeight = (int) (250 / aspectRatio); // Adjust height according to aspect ratio
            if (newHeight > 250) {
                newHeight = 250;
                newWidth = (int) (250 * aspectRatio); // Adjust width to maintain aspect ratio
            }

            // Resize the image for the thumbnail
            Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 30, thumbBaos);  // Compress at 30% quality
            byte[] thumbData = thumbBaos.toByteArray();

            // Step 5: Upload Full Image
            fullImageRef.putBytes(fullImageData).addOnSuccessListener(taskSnapshot ->
                    fullImageRef.getDownloadUrl().addOnSuccessListener(fullUrl -> {
                        // Step 6: Upload Thumbnail
                        thumbnailRef.putBytes(thumbData).addOnSuccessListener(thumbSnapshot ->
                                thumbnailRef.getDownloadUrl().addOnSuccessListener(thumbUrl -> {
                                    // Step 7: Save both URLs to Firestore
                                    savePhotoToFirestore(firebaseAuthManager.getCurrentUser().getUid(),
                                            fullUrl.toString(), thumbUrl.toString(), caption, getSelectedCategory(), lat, lng);
                                }));
                    }));
        } catch (IOException e) {
            Log.e("UploadError", "Failed to process image: " + e.getMessage());
        }
    }


    private Bitmap rotateImageIfRequired(Bitmap img, Uri uri) throws IOException {
        ExifInterface exif = new ExifInterface(Objects.requireNonNull(uri.getPath()));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            matrix.postRotate(180);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            matrix.postRotate(270);
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle(); // Recycle the original bitmap to free memory
        return rotatedBitmap;
    }


    //Image URL to be saved in Firestore
    private void savePhotoToFirestore(String userId, String imageUrl, String thumbnailUrl, String caption, String selectedCategory, Double lat, Double lng) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("user", userId);
        data.put("imageUrl", imageUrl);
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("caption", caption);
        data.put("category", selectedCategory);
        data.put("latitude", lat);
        data.put("longitude", lng);
        data.put("timestamp", FieldValue.serverTimestamp()); // Firestore server timestamp
        data.put("uploadType", "photo");

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
