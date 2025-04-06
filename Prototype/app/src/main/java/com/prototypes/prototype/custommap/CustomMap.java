package com.prototypes.prototype.custommap;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirebaseStorageManager;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomMap {
    private String name;
    private String owner;
    private String imageUrl;
    private String id;
    private ArrayList<String> stories;

    public CustomMap() {
        this.stories = new ArrayList<>();
    }
    public CustomMap(String name, String owner, String imageUrl, String id) {
        this.name = name;
        this.owner = owner;
        this.imageUrl = imageUrl;
        this.id = id;
        this.stories = new ArrayList<>();
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

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public ArrayList<String> getStories() {return stories;}

    public void setStories(ArrayList<String> stories) {this.stories = stories;}

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
                customMap.setStories(new ArrayList<>()); // Initialize stories array
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

    public static void updateMap(String mapId, String name, String imageUrl, ArrayList<String> storyIds, Activity activity) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("imageUrl", imageUrl);
        updates.put("stories", storyIds);

        db.collection("map").document(mapId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Map updated successfully", Toast.LENGTH_SHORT).show();
                    activity.finish();
                })
                .addOnFailureListener(e -> {
                    Log.d("UpdateMap", "Failed to update map: " + e.getMessage());
                    Toast.makeText(activity, "Failed to update map", Toast.LENGTH_SHORT).show();
                });
    }

}
