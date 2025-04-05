package com.prototypes.prototype.custommap;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirebaseStorageManager;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.security.acl.Owner;
import java.util.UUID;

public class CustomMap {
    private String name;
    private String owner;
    private String imageUrl;
    private String id;

    public CustomMap() {}
    public CustomMap(String name, String owner, String imageUrl, String id) {
        this.name = name;
        this.owner = owner;
        this.imageUrl = imageUrl;
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public static void creatMap(Activity activity, Uri file, String mapName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(activity);
        FirebaseStorageManager firebaseStorageManager = new FirebaseStorageManager();
        FirestoreManager firestoreManager = new FirestoreManager(db, CustomMap.class);
        String TAG = "Creat Map Function";

        String fileName = UUID.randomUUID().toString();
        // Add photo to storage
        firebaseStorageManager.uploadFileOutURL(file, "map/" + fileName, new FirebaseStorageManager.UploadFileCallback() {
            @Override
            public void onSuccess(String url) {
                //placeholder
                CustomMap customMap = new CustomMap(mapName, firebaseAuthManager.getCurrentUser().getUid(), url, "");
                firestoreManager.writeDocumentWithId("map", null, customMap, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Map added to firestore");
                        activity.finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "Failed to add map to firestore: " + e.toString());
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.d(TAG, "UploadCover Function Failed: " + error);
            }
        });
    }
}
