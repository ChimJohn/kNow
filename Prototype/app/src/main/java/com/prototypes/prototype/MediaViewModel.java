package com.prototypes.prototype;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prototypes.prototype.classes.Story;
import com.prototypes.prototype.firebase.FirebaseStorageManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.signup.SignUpActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MediaViewModel extends AndroidViewModel {
    private final MutableLiveData<String> mediaUrlLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> thumbnailUrlLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isUploading = new MutableLiveData<>(false);
    private final MediatorLiveData<Pair<String, String>> allDataReady = new MediatorLiveData<>();
    private final FirebaseStorageManager firebaseStorageManager;
    private final FirestoreManager<Story> firestoreManager;

    public MediaViewModel(Application application) {
        super(application);
        firebaseStorageManager = new FirebaseStorageManager();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        firestoreManager = new FirestoreManager(db, Story.class);
        allDataReady.addSource(mediaUrlLiveData, newMediaUrl -> {
            String latestThumbnailUrl = thumbnailUrlLiveData.getValue();
            if (newMediaUrl != null && latestThumbnailUrl != null) {
                allDataReady.setValue(new Pair<>(newMediaUrl, latestThumbnailUrl));
            }
        });
        allDataReady.addSource(thumbnailUrlLiveData, newThumbnailUrl -> {
            String latestMediaUrl = mediaUrlLiveData.getValue();
            if (newThumbnailUrl != null && latestMediaUrl != null) {
                allDataReady.setValue(new Pair<>(latestMediaUrl, newThumbnailUrl));
            }
        });
    }
    public void uploadMediaAndThumbnailInBackground(Uri mediaUri) {
        isUploading.setValue(true);
        new Thread(() -> {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final StorageReference thumbnailRef = storage.getReference("Thumbnails/" + UUID.randomUUID().toString());
            try {
                if (mediaUri.toString().endsWith(".mp4")) {
                    final StorageReference mediaRef = storage.getReference("Videos/" + UUID.randomUUID().toString());
                    Task<Uri> mediaUploadTask = mediaRef.putFile(mediaUri)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return mediaRef.getDownloadUrl();
                            });
                    Tasks.whenAllSuccess(mediaUploadTask, createVideoThumbnail(mediaUri))
                            .addOnSuccessListener(results -> {
                                Log.d("LOL", "CMON");
                                setMediaUrl(results.get(0).toString());
                                setThumbnailUrl(results.get(1).toString());
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "File upload failed: " + e.getMessage()));
                } else {
                    final StorageReference mediaRef = storage.getReference("Images/" + UUID.randomUUID().toString());
                    Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), mediaUri);
                    originalBitmap = Story.fixImageRotation(originalBitmap, mediaUri);
                    Bitmap fullImageBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);  // Create a mutable copy of the original image
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    fullImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);  // Compress at 50% quality
                    float aspectRatio = (float) originalBitmap.getWidth() / (float) originalBitmap.getHeight();
                    int newWidth = 250;
                    int newHeight = (int) (250 / aspectRatio); // Adjust height according to aspect ratio
                    if (newHeight > 250) {
                        newHeight = 250;
                        newWidth = (int) (250 * aspectRatio); // Adjust width to maintain aspect ratio
                    }
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
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "Thumbnail upload failed: " + e.getMessage()));
                    Task<Uri> mediaUploadTask = mediaRef.putFile(mediaUri)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException(); // Prevent silent failure
                                }
                                return mediaRef.getDownloadUrl();
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "Media upload failed: " + e.getMessage()));
                    Tasks.whenAllSuccess(mediaUploadTask, thumbnailUploadTask)
                            .addOnSuccessListener(results -> {
                                Log.d("LOL", "CMON");
                                setMediaUrl(results.get(0).toString());
                                setThumbnailUrl(results.get(1).toString());
                            })
                            .addOnFailureListener(e -> Log.e("UploadError", "File upload failed: " + e.getMessage()));
                }
            } catch (IOException e) {
                Log.e("UploadError", "Failed to process image: " + e.getMessage());
            } finally {
                isUploading.postValue(false);
            }
        }).start();
    }
    public MediatorLiveData<Pair<String, String>> getAllDataReady() {
        return allDataReady;
    }
    public void setMediaUrl(String url) {
        mediaUrlLiveData.postValue(url);
    }
    public void setThumbnailUrl(String url) {
        thumbnailUrlLiveData.postValue(url);
    }
    public void saveMediaToFirebaseStorage(String userId, String caption, String selectedCategory, Double lat, Double lng, String mediaType) {
            Observer<Pair<String, String>> firestoreObserver = new Observer<>() {
                @Override
                public void onChanged(Pair<String, String> pair) {
                    if (pair != null) {
                        firestoreManager.writeDocument(
                                "media",
                                null,
                                new Story(null, userId, caption, selectedCategory, pair.first, lat, lng, mediaType, pair.second),
                                new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onFailure(Exception e) {}
                                });
                        mediaUrlLiveData.setValue(null);
                        thumbnailUrlLiveData.setValue(null);
                        allDataReady.setValue(null);
                        getAllDataReady().removeObserver(this);
                    }
                }
            };
            getAllDataReady().observeForever(firestoreObserver);
    }

    private Task<Uri> createVideoThumbnail(Uri videoUri) throws IOException {
        TaskCompletionSource<Uri> taskCompletionSource = new TaskCompletionSource<>();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        try {
            mediaMetadataRetriever.setDataSource(getApplication(), videoUri);
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            File thumbnailFile = new File(getApplication().getCacheDir(), "thumbnail.jpg");

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
            firebaseStorageManager.uploadFileOutURL(Uri.fromFile(thumbnailFile), "Thumbnails/" + UUID.randomUUID().toString() + ".jpg", new FirebaseStorageManager.UploadFileCallback() {
                @Override
                public void onSuccess(String url) {
                    taskCompletionSource.setResult(Uri.parse(url));
                }
                @Override
                public void onFailure(String error) {
                    taskCompletionSource.setException(new Exception(error));
                }
            });
        } catch (Exception e) {
            Log.e("ThumbnailError", "Error creating thumbnail: " + e.getMessage());
            taskCompletionSource.setException(e);
        } finally {
            mediaMetadataRetriever.release();
        }

        return taskCompletionSource.getTask();
    }
}
