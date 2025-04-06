package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.prototypes.prototype.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class StoryViewAdapter extends RecyclerView.Adapter<StoryViewAdapter.StoryViewHolder> {
    private final ArrayList<Story> storyList;
    private final Context context;

    public interface OnGpsClickListener {
        void onGpsClick(double latitude, double longitude);
    }
    private final OnGpsClickListener gpsClickListener;

    public StoryViewAdapter(Context context, ArrayList<Story> stories, OnGpsClickListener gpsClickListener) {
        this.context = context;
        this.storyList = stories;
        this.gpsClickListener = gpsClickListener;
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
        holder.bind(story, gpsClickListener, position, storyList.size()); // pass position + total count
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
        holder.prepareAndPlayVideo();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull StoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.releasePlayer(); // Add this line
    }
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final PlayerView playerView;
        private final ProgressBar imageLoader;
        private ExoPlayer exoPlayer;
        private Story story;

        public StoryViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.story_image);
            playerView = itemView.findViewById(R.id.player_view);
            imageLoader = itemView.findViewById(R.id.image_loader);
        }

        public void bind(Story story, OnGpsClickListener gpsClickListener, int position, int totalCount) {
            this.story = story;
            if (!story.isVideo()) {
                playerView.setVisibility(View.GONE);
                imageLoader.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(story.getMediaUrl())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                imageLoader.setVisibility(View.GONE);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                imageLoader.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(imageView);
            }
        }
        public void prepareAndPlayVideo() {
            if (!story.isVideo()){
                return;
            }
            imageView.setVisibility(View.GONE);
            imageLoader.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.INVISIBLE);
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
                playerView.setPlayer(exoPlayer);
            }
            MediaItem mediaItem = MediaItem.fromUri(story.getMediaUrl());
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    switch (state) {
                        case Player.STATE_BUFFERING:
                            imageView.setVisibility(View.INVISIBLE);
                            imageLoader.setVisibility(View.VISIBLE);
                            playerView.setVisibility(View.GONE);
                            break;
                        case Player.STATE_READY:
                            long duration = exoPlayer.getDuration();
                            if (duration <= 0) {
                                imageView.setVisibility(View.VISIBLE);
                                imageLoader.setVisibility(View.VISIBLE);
                                playerView.setVisibility(View.GONE);
                                loadFirstFrame(story.getMediaUrl(), imageView);
                            } else {
                                playerView.setVisibility(View.VISIBLE);
                                imageView.setVisibility(View.GONE);
                                imageLoader.setVisibility(View.GONE);
                            }
                            break;
                    }
                }
            });
            exoPlayer.prepare();
        }
        private void loadFirstFrame(String videoUrl, ImageView imageView) {
            new Thread(() -> {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(videoUrl, new HashMap<>());
                    Bitmap bitmap = retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (bitmap != null) {
                        imageView.post(() -> {
                            Glide.with(imageView.getContext())
                                    .load(bitmap)
                                    .into(imageView);
                            imageLoader.setVisibility(View.GONE);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        retriever.release();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
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
