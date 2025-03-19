package com.prototypes.prototype;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MediaViewModel extends ViewModel {
    private final MutableLiveData<String> imageUrlLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> thumbnailUrlLiveData = new MutableLiveData<>();
    private final MediatorLiveData<Pair<String, String>> allDataReady = new MediatorLiveData<>();

    public MediaViewModel() {
        // Observe image URL
        allDataReady.addSource(imageUrlLiveData, newImageUrl -> {
            String latestThumbnailUrl = thumbnailUrlLiveData.getValue();
            if (newImageUrl != null && latestThumbnailUrl != null) {
                allDataReady.setValue(new Pair<>(newImageUrl, latestThumbnailUrl));
            }
        });

        // Observe thumbnail URL
        allDataReady.addSource(thumbnailUrlLiveData, newThumbnailUrl -> {
            String latestImageUrl = imageUrlLiveData.getValue();
            if (newThumbnailUrl != null && latestImageUrl != null) {
                allDataReady.setValue(new Pair<>(latestImageUrl, newThumbnailUrl));
            }
        });
    }

    public LiveData<String> getImageUrlLiveData() {
        return imageUrlLiveData;
    }

    public LiveData<String> getThumbnailUrlLiveData() {
        return thumbnailUrlLiveData;
    }

    public MediatorLiveData<Pair<String, String>> getAllDataReady() {
        return allDataReady;
    }

    public void setImageUrl(String url) {
        imageUrlLiveData.postValue(url);
    }

    public void setThumbnailUrl(String url) {
        thumbnailUrlLiveData.postValue(url);
    }

    public void savePhotoToFirestore(String userId, String caption, String selectedCategory, Double lat, Double lng) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Observer<Pair<String, String>> firestoreObserver = new Observer<Pair<String, String>>() {
            @Override
            public void onChanged(Pair<String, String> pair) {
                saveMediaToFirestore(db, pair.first, pair.second, userId, caption, selectedCategory, lat, lng);
                // Remove observer after saving data
                getAllDataReady().removeObserver(this);
            }
        };

        // Attach the observer
        getAllDataReady().observeForever(firestoreObserver);

    }

    private void saveMediaToFirestore(FirebaseFirestore db, String imageUrl, String thumbnailUrl, String userId, String caption, String selectedCategory, Double lat, Double lng) {
        Map<String, Object> mediaData = new HashMap<>();
        mediaData.put("imageUrl", imageUrl);
        mediaData.put("thumbnailUrl", thumbnailUrl);
        mediaData.put("userId", userId);
        mediaData.put("caption", caption);
        mediaData.put("category", selectedCategory);
        mediaData.put("latitude", lat);
        mediaData.put("longitude", lng);
        mediaData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("media").add(mediaData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Document added: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding document", e));
    }

}
