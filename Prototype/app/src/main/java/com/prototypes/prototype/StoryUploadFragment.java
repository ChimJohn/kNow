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
    private ImageView imageView;
    private EditText captionEditText;
    private Button saveButton;
    private ChipGroup categoryChipGroup;
    private static final String ARG_MEDIA_URI = "mediaUri";
    private CurrentLocationViewModel currentLocationViewModel;
    private FirebaseAuthManager firebaseAuthManager;
    double lat, lng;
    private MediaViewModel mediaViewModel;
    private Context appContext;

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
        uploadMediaAndThumbnailInBackground(mediaUri);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_story_upload, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mediaViewModel = new ViewModelProvider(requireActivity()).get(MediaViewModel.class);
        currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);
        firebaseAuthManager = new FirebaseAuthManager(requireActivity());

        imageView = view.findViewById(R.id.imageView);
        captionEditText = view.findViewById(R.id.captionEditText);
        categoryChipGroup = view.findViewById(R.id.chipGroup);
        saveButton = view.findViewById(R.id.saveButton);

        String userId = firebaseAuthManager.getCurrentUser().getUid();
        String mediaUriString = getArguments().
                getString(ARG_MEDIA_URI, "");
        Uri mediaUri = Uri.parse(mediaUriString);
        if (mediaUri.toString().endsWith(".mp4")) {
            // For videos, extract the first frame
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), mediaUri);
                // Get the first frame (at time 0)
                Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bitmap != null) {
                    // Set the first frame as the image in the ImageView
                    imageView.setImageBitmap(bitmap);
                }
                retriever.release(); // Don't forget to release the retriever
            } catch (Exception e) {
                Log.e("VideoThumbnailError", "Error extracting video thumbnail: " + e.getMessage());
            }
        } else {
            // For images, use Glide to display it
            Glide.with(requireContext()) // Using Glide to load the image
                    .load(mediaUri)
                    .into(imageView);
        }
        Location lastKnownLocation = currentLocationViewModel.getLastKnownLocation();
        lat = lastKnownLocation.getLatitude();
        lng = lastKnownLocation.getLongitude();

        // Handle save button click
        saveButton.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString();
            if (mediaUri.toString().endsWith(".mp4")) {
                uploadVideoToFirebaseStorage(mediaUri, caption);
            } else {
                mediaViewModel.savePhotoToFirestore(userId, caption, getSelectedCategory(), lat, lng);
            }
            ExploreFragment exploreFragment = new ExploreFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, exploreFragment) // Replace with the correct container ID
                    .addToBackStack(null) // Optional, allows going back to previous fragment
                    .commit();
            Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadMediaAndThumbnailInBackground(Uri mediaUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference mediaRef;
            StorageReference thumbnailRef;
            if (mediaUri.toString().endsWith(".mp4")) {
                mediaRef = storage.getReference("Videos/" + UUID.randomUUID().toString());
            } else {
                //BACKGROUND UPLOAD FOR PHOTOS
                mediaRef = storage.getReference("Images/" + UUID.randomUUID().toString());
                thumbnailRef = storage.getReference("Thumbnails/" + UUID.randomUUID().toString());
                try {
                    // Get the original bitmap from the URI
                    Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), mediaUri);
                    // Handle EXIF orientation
                    // Step 1: Rotate Image (if necessary)
                    originalBitmap = rotateImageIfRequired(originalBitmap, mediaUri);
                    // Step 2: Create a copy of the original image without altering resolution
                    Bitmap fullImageBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);  // Create a mutable copy of the original image
                    // Step 3: Compress and convert the full image for storage
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    fullImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);  // Compress at 50% quality
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

                    Task<Uri> thumbnailUploadTask = thumbnailRef.putBytes(thumbData)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException(); // Prevent silent failure
                                }
                                return thumbnailRef.getDownloadUrl();
                            }).addOnFailureListener(e -> Log.e("UploadError", "Thumbnail upload failed: " + e.getMessage()));

                    Task<Uri> mediaUploadTask = mediaRef.putFile(mediaUri)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException(); // Prevent silent failure
                                }
                                return mediaRef.getDownloadUrl();
                            }).addOnFailureListener(e -> Log.e("UploadError", "Media upload failed: " + e.getMessage()));

                    Tasks.whenAllSuccess(mediaUploadTask, thumbnailUploadTask)
                            .addOnSuccessListener(results -> {
                                Log.d("LOL", "CMON");
                                String mediaUrl = ((Uri) results.get(0)).toString();
                                String thumbnailUrl = ((Uri) results.get(1)).toString();
                                mediaViewModel.setImageUrl(mediaUrl);
                                mediaViewModel.setThumbnailUrl(thumbnailUrl);
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "File upload failed: " + e.getMessage()));
                } catch (IOException e) {
                    Log.e("UploadError", "Failed to process image: " + e.getMessage());
                }
            }
        });
    }


    public interface ThumbnailUploadCallback {
        void onThumbnailUploaded(String thumbUrl);
    }

    private void uploadVideoToFirebaseStorage(Uri mediaUri, String caption) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference mediaRef;
        // Reference for storing the video file
        mediaRef = storage.getReference("Videos/" + UUID.randomUUID().toString());
        // Upload the video to Firebase Storage
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

    private void savePhotoToFirestore(String userId, String caption, String selectedCategory, Double lat, Double lng) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        LiveData<String> imageUrlLiveData = mediaViewModel.getImageUrlLiveData();
        LiveData<String> thumbnailUrlLiveData = mediaViewModel.getThumbnailUrlLiveData();
        if (imageUrlLiveData.getValue() != null && thumbnailUrlLiveData.getValue() != null) {
            // Both URLs are ready, save immediately
            saveMediaToFirestore(db, imageUrlLiveData.getValue(), thumbnailUrlLiveData.getValue(), userId, caption, selectedCategory, lat, lng);
            return;
        }
        // MediatorLiveData to observe both image and thumbnail URLs
        MediatorLiveData<Pair<String, String>> allDataReady = new MediatorLiveData<>();

        // Observe image URL
        allDataReady.addSource(imageUrlLiveData, newImageUrl -> {
            String latestThumbnailUrl = thumbnailUrlLiveData.getValue();
            if (newImageUrl != null && latestThumbnailUrl != null) {
                // Once both URLs are available, save to Firestore
                allDataReady.setValue(new Pair<>(newImageUrl, latestThumbnailUrl));
            }
        });

        // Observe thumbnail URL
        allDataReady.addSource(thumbnailUrlLiveData, newThumbnailUrl -> {
            String latestImageUrl = imageUrlLiveData.getValue();
            if (newThumbnailUrl != null && latestImageUrl != null) {
                // Once both URLs are available, save to Firestore
                allDataReady.setValue(new Pair<>(latestImageUrl, newThumbnailUrl));
            }
        });

        // When both URLs are ready, proceed to save
        allDataReady.observe(getViewLifecycleOwner(), pair -> {
            saveMediaToFirestore(db, pair.first, pair.second, userId, caption, selectedCategory, lat, lng);
            // Remove sources after data is saved
            allDataReady.removeSource(imageUrlLiveData);
            allDataReady.removeSource(thumbnailUrlLiveData);
        });
    }

    private void saveMediaToFirestore(FirebaseFirestore dbInstance, String userId, String mediaUrl, String thumbnailUrl, String caption, String selectedCategory, Double lat, Double lng){
        Map<String, Object> data = new HashMap<>();
        data.put("user", userId);
        data.put("caption", caption);
        data.put("category", selectedCategory);
        data.put("latitude", lat);
        data.put("longitude", lng);
        data.put("timestamp", FieldValue.serverTimestamp()); // Firestore server timestamp
        data.put("uploadType", "photo");
        data.put("mediaUrl", mediaUrl);
        data.put("thumbnailUrl", thumbnailUrl);
        dbInstance.collection("media").document()
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
