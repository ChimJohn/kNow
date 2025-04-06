package com.prototypes.prototype.custommap.editmaps;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.story.Story;

import java.util.ArrayList;

public class StorySelectionAdapter extends RecyclerView.Adapter<StorySelectionAdapter.StoryViewHolder> {
    private Context context;
    private ArrayList<Story> stories;
    private CustomMap map;
    private ArrayList<String> selectedStoryIds = new ArrayList<>();

    public StorySelectionAdapter(Context context, ArrayList<Story> stories, CustomMap map) {
        this.context = context;
        this.stories = stories;
        this.map = map;

        // Initialize selected stories from map
        if (map.getStories() != null) {
            selectedStoryIds.addAll(map.getStories());
        }
    }

    public ArrayList<String> getSelectedStoryIds() {
        return selectedStoryIds;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.story_selection_item, parent, false);
        return new StoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = stories.get(position);

        // Ensure the story has a valid ID
        if (story.getId() == null || story.getId().isEmpty()) {
            Log.e("StorySelectionAdapter", "Story ID is null or empty!");
            return; // Skip invalid stories
        }

        // Load story thumbnail
        Glide.with(context)
                .load(story.getMediaUrl())
                .into(holder.storyImage);

        holder.storyCaption.setText(story.getCaption());

        // Check if story is already in the map
        boolean isSelected = selectedStoryIds.contains(story.getId());
        holder.checkBox.setChecked(isSelected);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedStoryIds.contains(story.getId())) {
                    selectedStoryIds.add(story.getId());
                }
            } else {
                selectedStoryIds.remove(story.getId());
            }

            // Update the map's stories array in Firestore
            updateMapStories();
        });
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    private void updateMapStories() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("map").document(map.getId())
                .update("stories", selectedStoryIds)
                .addOnSuccessListener(aVoid -> {
                    Log.d("StorySelectionAdapter", "Stories updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("StorySelectionAdapter", "Failed to update stories: " + e.getMessage());
                });
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImage;
        TextView storyCaption;
        CheckBox checkBox;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            storyImage = itemView.findViewById(R.id.ivStory);
            storyCaption = itemView.findViewById(R.id.tvStoryCaption);
            checkBox = itemView.findViewById(R.id.cbSelect);
        }
    }
}
