package com.prototypes.prototype.firebase;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import android.util.Log;

public class FirebaseStorageManager {
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private static final String TAG = "FirebaseStorageManager";

    public FirebaseStorageManager() {
        // Initialize Firebase Storage
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = mFirebaseStorage.getReference();
    }

    public void uploadFile(Uri fileUri, String storagePath, UploadFileCallback callback) {
        StorageReference fileReference = mStorageReference.child(storagePath);

        fileReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "File uploaded successfully.");
                    // Once the file is uploaded, retrieve the download URL
                    getDownloadUrl(storagePath, new DownloadUrlCallback() {
                        @Override
                        public void onSuccess(String url) {
                            // Pass the URL to the callback
                            callback.onSuccess(url);
                        }

                        @Override
                        public void onFailure(String error) {
                            // If URL retrieval fails, pass the error to the callback
                            callback.onFailure(error);
                        }
                    });
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "File upload failed: " + exception.getMessage());
                    // Pass the failure message to the callback
                    callback.onFailure(exception.getMessage());
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Upload is " + progress + "% done");
                });
    }

    // Method to retrieve the download URL
    public void getDownloadUrl(String storagePath, DownloadUrlCallback callback) {
        StorageReference fileReference = mStorageReference.child(storagePath);

        fileReference.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "Download URL retrieved successfully.");
                    callback.onSuccess(uri.toString());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to retrieve download URL: " + exception.getMessage());
                    callback.onFailure(exception.getMessage());
                });
    }

    // Callback interface for file upload success/failure
    public interface UploadFileCallback {
        void onSuccess(String url); // Called when the file upload is successful and URL is returned
        void onFailure(String error); // Called when the file upload fails
    }

    // Callback interface for downloading URLs
    public interface DownloadUrlCallback {
        void onSuccess(String url); // Called when the URL retrieval is successful
        void onFailure(String error); // Called when the URL retrieval fails
    }
}
