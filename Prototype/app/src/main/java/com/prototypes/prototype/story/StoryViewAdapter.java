package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.prototypes.prototype.R;

import java.util.ArrayList;

public class StoryViewAdapter extends RecyclerView.Adapter<StoryViewAdapter.StoryViewHolder> {
    private final ArrayList<Story> storyList;
    private final Context context;
    private final ViewPager2 viewPager2;
    private final Handler autoScrollHandler;
    private Runnable autoScrollRunnable;


    public interface OnGpsClickListener {
        void onGpsClick(double latitude, double longitude);
    }
    private final OnGpsClickListener gpsClickListener;

    public StoryViewAdapter(Context context, ArrayList<Story> stories, OnGpsClickListener gpsClickListener, ViewPager2 viewPager2) {
        this.context = context;
        this.storyList = stories;
        setHasStableIds(true);
        this.gpsClickListener = gpsClickListener;
        this.viewPager2 = viewPager2;
        this.autoScrollHandler = new Handler(Looper.getMainLooper());
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
            hash = 31 * hash + firebaseId.charAt(i);
        }
        return Math.abs(hash);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);
        holder.bind(story, gpsClickListener);
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    @Override
    public void onViewRecycled(@NonNull StoryViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull StoryViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
        autoScrollRunnable = () -> {
            int nextItem = viewPager2.getCurrentItem() + 1;
            if (nextItem < getItemCount()) {
                viewPager2.setCurrentItem(nextItem, true);
            } else {
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 4000);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull StoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pausePlayer();
    }
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final PlayerView playerView;
        private final TextView storyCaption;
        private final Button gpsButton;
        private ExoPlayer exoPlayer;
        private final ProgressBar imageLoader;

        public StoryViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.story_image);
            playerView = itemView.findViewById(R.id.player_view);
            storyCaption = itemView.findViewById(R.id.story_snippet);
            gpsButton = itemView.findViewById(R.id.btnGps);
            imageLoader = itemView.findViewById(R.id.image_loader);
        }

        public void bind(Story story, OnGpsClickListener gpsClickListener) {
            storyCaption.setText(story.getCaption());
            gpsButton.bringToFront();
            gpsButton.setOnClickListener(v -> {
                if (gpsClickListener != null) {
                    gpsClickListener.onGpsClick(story.getLatitude(), story.getLongitude());
                }
            });
            if (story.isVideo()) {
                imageView.setVisibility(View.GONE);
                imageLoader.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                if (exoPlayer == null) {
                    exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
                    playerView.setPlayer(exoPlayer);
                }
                MediaItem mediaItem = MediaItem.fromUri(story.getMediaUrl());
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
                playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
                    if (visibility == View.VISIBLE) {
                        playerView.setBackgroundColor(Color.TRANSPARENT); // Remove dim effect
                    }
                });
            } else {
                playerView.setVisibility(View.GONE);
                imageLoader.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(story.getMediaUrl())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                imageLoader.setVisibility(View.GONE); // Hide loader on failure
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                imageLoader.setVisibility(View.GONE); // Hide loader when done
                                return false;
                            }
                        })
                        .into(imageView);
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
