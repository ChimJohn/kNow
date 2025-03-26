package com.prototypes.prototype.story;

import android.content.Context;
import android.util.Log;
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
    private final ArrayList<Story> storyList;
    private Context context;
    private final ArrayList<ExoPlayer> playerCache;
    private int currentPosition = 0; // Track the current page


    public StoryViewAdapter(Context context,  ArrayList<Story> stories) {
        this.context = context;
        this.storyList = stories;
        this.playerCache = new ArrayList<>();
        setHasStableIds(true);
        preloadVideos();
    }
    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_view, parent, false);
        return new StoryViewHolder(view);
    }
    @Override
    public long getItemId(int position) {
        return stringToLongHash(storyList.get(position).getId());
    }
    private long stringToLongHash(String firebaseId) {
        long hash = 0;
        for (int i = 0; i < firebaseId.length(); i++) {
            hash = 31 * hash + firebaseId.charAt(i); // Simple but effective hash function
        }
        return Math.abs(hash); // Ensure positive values
    }
    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        Log.d("POSITION", String.valueOf(position));
        holder.bind(story, playerCache.get(position));
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
    @Override
    public void onViewAttachedToWindow(@NonNull StoryViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.restartVideo(); // Restart the video every time it's visible
    }
    @Override
    public void onViewDetachedFromWindow(@NonNull StoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pausePlayer(); // Pause player when the view is detached
    }
    private void preloadVideos() {
        for (Story story : storyList) {
            if (story.isVideo()) {
                ExoPlayer player = new ExoPlayer.Builder(context).build();
                MediaItem mediaItem = MediaItem.fromUri(story.getMediaUrl());
                player.setMediaItem(mediaItem);
                player.prepare(); // Preload the video
                playerCache.add(player);
            } else {
                playerCache.add(null); // Placeholder for images
            }
        }
    }
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
        notifyDataSetChanged(); // Refresh adapter when page changes
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
        public void bind(Story story, ExoPlayer preloadedPlayer) {
            storyCaption.setText(story.getCaption());
            if (story.isVideo()) {
                storyImage.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                this.exoPlayer = preloadedPlayer;
                playerView.setPlayer(exoPlayer);
                playerView.setUseController(false);
                if (exoPlayer != null && !exoPlayer.isPlaying()) {
                    exoPlayer.play();
                }
            } else {
                playerView.setVisibility(View.GONE);
                storyImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(story.getMediaUrl()).into(storyImage);
            }
        }
        public void restartVideo() {
            if (exoPlayer != null) {
                exoPlayer.seekTo(0);
                exoPlayer.play();
            }
        }
        public void pausePlayer() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
            }
        }
        public void releasePlayer() {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        }
    }
}
