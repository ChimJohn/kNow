package com.prototypes.prototype.story;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

import java.util.ArrayList;


public class StoryViewAdapter extends RecyclerView.Adapter<StoryViewAdapter.StoryViewHolder> {
    private final ArrayList<Story> storyList; // List of stories
    private Context context;
    private StoryListener listener;
    public StoryViewAdapter(Context context,  ArrayList<Story> stories, StoryListener listener) {
        this.context = context;
        this.storyList = stories;
        this.listener = listener;
    }
    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_view, parent, false);
        return new StoryViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        holder.bind(story);
    }
    @Override
    public int getItemCount() {
        return storyList.size();
    }
    @Override
    public void onViewRecycled(@NonNull StoryViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer(); // Release player when the view is recycled
    }
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView storyImage;
        private PlayerView playerView;
        private ExoPlayer exoPlayer;
        private TextView storyCaption;
        public StoryViewHolder(View itemView) {
            super(itemView);
            storyImage = itemView.findViewById(R.id.story_image);
            playerView = itemView.findViewById(R.id.player_view);
            storyCaption = itemView.findViewById(R.id.story_snippet);
        }
        public void bind(Story story) {
            storyCaption.setText(story.getCaption());
            if (story.isVideo()) {
                storyImage.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                initializePlayer(story.getMediaUrl());
            } else {
                playerView.setVisibility(View.GONE);
                storyImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(story.getMediaUrl()).into(storyImage);
            }
        }
        private void initializePlayer(String videoUrl) {
            exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
            playerView.setPlayer(exoPlayer);
            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
        }
        public void releasePlayer() {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        }
    }
    public interface StoryListener {
        void onStoryTap(int position);
    }
}
