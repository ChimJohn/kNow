package com.prototypes.prototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FieldValue;

import java.io.File;
import java.io.FileOutputStream;
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
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoryUploadFragment extends Fragment {
    private static final String ARG_MEDIA_URI = "mediaUri";
    private ImageView imageView;
    private EditText captionEditText;
    private Button saveButton;
    private ChipGroup categoryChipGroup;
    private FirebaseAuthManager firebaseAuthManager;
    private MediaViewModel mediaViewModel;
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
        String mediaUriString = getArguments().getString(ARG_MEDIA_URI);
        Uri mediaUri = Uri.parse(mediaUriString); // Convert string back to URI

    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_story_upload, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mediaViewModel = new ViewModelProvider(requireActivity()).get(MediaViewModel.class);
        CurrentLocationViewModel currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);
        firebaseAuthManager = new FirebaseAuthManager(requireActivity());
        imageView = view.findViewById(R.id.imageView);
        captionEditText = view.findViewById(R.id.captionEditText);
        categoryChipGroup = view.findViewById(R.id.chipGroup);
        saveButton = view.findViewById(R.id.saveButton);
        String userId = firebaseAuthManager.getCurrentUser().getUid();
        String mediaUriString = getArguments().
                getString(ARG_MEDIA_URI, "");
        Uri mediaUri = Uri.parse(mediaUriString);
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
            if (mediaUri.toString().endsWith(".mp4")) {
                uploadVideoToFirebaseStorage(mediaUri, caption);
            } else {
                mediaViewModel.savePhotoToFirebaseStorage(userId, caption, getSelectedCategory(), lat, lng);
            }
            ExploreFragment exploreFragment = new ExploreFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, exploreFragment) // Replace with the correct container ID
                    .addToBackStack(null) // Optional, allows going back to previous fragment
                    .commit();
            Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
        });
    }

    public interface ThumbnailUploadCallback {
        void onThumbnailUploaded(String thumbUrl);
    }

    private void uploadVideoToFirebaseStorage(Uri mediaUri, String caption) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference mediaRef;
        mediaRef = storage.getReference("Videos/" + UUID.randomUUID().toString());
        mediaRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot ->
                        // After the video is uploaded successfully, get the download URL
                        mediaRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUrl -> {
                                    // Now create the thumbnail for the video
                                    createVideoThumbnail(mediaUri, thumbUrl -> {
                                        // Save video info to Firestore once the thumbnail URL is available
                                        saveVideoToFirestore(firebaseAuthManager.getCurrentUser().getUid(),
                                                downloadUrl.toString(),  // Full video URL
                                                thumbUrl,                // Thumbnail URL
                                                caption,                 // The caption provided by the user
                                                getSelectedCategory(),   // Category
                                                lat,                     // Latitude
                                                lng                      // Longitude
                                        );
                                    });
                                })
                                .addOnFailureListener(e -> Log.e("UploadError", "Error getting download URL: " + e.getMessage()))
                )
                .addOnFailureListener(e -> Log.e("UploadError", "Upload failed: " + e.getMessage()));
    }

    private void createVideoThumbnail(Uri videoUri, ThumbnailUploadCallback callback) {
        try {
            // Create a MediaMetadataRetriever instance
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getContext(), videoUri);
            Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            // Convert the Bitmap to a file and upload to Firebase Storage
            File thumbnailFile = new File(requireContext().getCacheDir(), "thumbnail.jpg");
            try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
                float aspectRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
                int newWidth = 250;
                int newHeight = (int) (250 / aspectRatio); // Adjust height according to aspect ratio
                if (newHeight > 250) {
                    newHeight = 250;
                    newWidth = (int) (250 * aspectRatio); // Adjust width to maintain aspect ratio
                }
                Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
            }
            retriever.release(); // Don't forget to release the retriever
            // Upload the thumbnail to Firebase Storage
            StorageReference thumbRef = FirebaseStorage.getInstance().getReference("Thumbnails/" + UUID.randomUUID().toString() + ".jpg");
            thumbRef.putFile(Uri.fromFile(thumbnailFile))
                    .addOnSuccessListener(taskSnapshot -> thumbRef.getDownloadUrl()
                            .addOnSuccessListener(thumbDownloadUrl -> {
                                // Notify the callback that the thumbnail is uploaded successfully
                                callback.onThumbnailUploaded(thumbDownloadUrl.toString());
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "Failed to get thumbnail URL: " + e.getMessage()))
                    )
                    .addOnFailureListener(e -> Log.e("UploadError", "Thumbnail upload failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.e("ThumbnailError", "Error creating thumbnail: " + e.getMessage());
        }
    }

    private void saveVideoToFirestore(String userId, String videoUrl, String thumbnailUrl, String caption, String selectedCategory, Double lat, Double lng) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("user", userId);
        data.put("imageUrl", videoUrl);
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("caption", caption);
        data.put("category", selectedCategory);
        data.put("latitude", lat);
        data.put("longitude", lng);
        data.put("timestamp", FieldValue.serverTimestamp()); // Firestore server timestamp
        data.put("uploadType", "video");

        db.collection("media").document()
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("UploadFragment", "Video URL saved to Firestore");
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
