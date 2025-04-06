package com.prototypes.prototype.custommap.editmaps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.firebase.FirebaseStorageManager;
import com.prototypes.prototype.story.Story;
import com.prototypes.prototype.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditMapDetails extends AppCompatActivity {
    ImageButton btnExit;
    EditText etMapName;
    TextView tvUpdate, tvSetCover;
    ImageView imgCover;
    Uri image;
    String mapName;
    String mapId;
    CustomMap currentMap;
    RecyclerView storyRecyclerView;
    ArrayList<Story> selectedStories = new ArrayList<>();

    private static final String TAG = "Edit Map Details Activity";

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        image = result.getData().getData();
                        Glide.with(getApplicationContext()).load(image).into(imgCover);
                    }
                } else {
                    Toast.makeText(EditMapDetails.this, "Please select an image", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map_details);

        btnExit = findViewById(R.id.btnExit);
        etMapName = findViewById(R.id.etMapName);
        tvUpdate = findViewById(R.id.tvUpdate);
        tvSetCover = findViewById(R.id.tvSetCover);
        imgCover = findViewById(R.id.ivCustomMap);
        storyRecyclerView = findViewById(R.id.rvStories);

        // Get map ID from intent
        mapId = getIntent().getStringExtra("mapId");

        // Load current map data
        loadMapData();

        // Load user stories for selection
        loadUserStories();

        // Exit button
        btnExit.setOnClickListener(v -> finish());

        // Update map button
        tvUpdate.setOnClickListener(v -> {
            if (etMapName.getText().toString().trim().equals("")) {
                Toast.makeText(EditMapDetails.this, "Fill in map name!", Toast.LENGTH_SHORT).show();
                return;
            }

            mapName = etMapName.getText().toString().trim();
            updateMap();
        });

        // Select cover image
        tvSetCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        });
    }

    private void loadMapData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("map").document(mapId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentMap = documentSnapshot.toObject(CustomMap.class);
                    if (currentMap != null) {
                        etMapName.setText(currentMap.getName());
                        Glide.with(this)
                                .load(currentMap.getImageUrl())
                                .into(imgCover);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error loading map: " + e.getMessage());
                    Toast.makeText(this, "Failed to load map details", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserStories() {
        User.getStories(this, new User.UserCallback<Story>() {
            @Override
            public void onMapsLoaded(ArrayList<Story> stories) {
                StorySelectionAdapter adapter = new StorySelectionAdapter(EditMapDetails.this, stories, currentMap);
                storyRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Error loading stories: " + e.getMessage());
                Toast.makeText(EditMapDetails.this, "Failed to load stories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMap() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // If new image is selected, upload it first
        if (image != null) {
            String fileName = UUID.randomUUID().toString();
            FirebaseStorageManager storageManager = new FirebaseStorageManager();

            storageManager.uploadFileOutURL(image, "map/" + fileName, new FirebaseStorageManager.UploadFileCallback() {
                @Override
                public void onSuccess(String url) {
                    // Update map with new image URL
                    updateMapInFirestore(db, url);
                }

                @Override
                public void onFailure(String error) {
                    Log.d(TAG, "Failed to upload image: " + error);
                    Toast.makeText(EditMapDetails.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Update map without changing image
            updateMapInFirestore(db, currentMap.getImageUrl());
        }
    }

    private void updateMapInFirestore(FirebaseFirestore db, String imageUrl) {
        // Get selected stories from adapter
        StorySelectionAdapter adapter = (StorySelectionAdapter) storyRecyclerView.getAdapter();
        ArrayList<String> selectedStoryIds = adapter != null ?
                adapter.getSelectedStoryIds() : new ArrayList<>();

        // Create map update object
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", mapName);
        updates.put("imageUrl", imageUrl);
        updates.put("stories", selectedStoryIds); // Add stories

        // Update the map document
        db.collection("map").document(mapId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Map updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error updating map: " + e.getMessage());
                    Toast.makeText(this, "Failed to update map", Toast.LENGTH_SHORT).show();
                });
    }

}

