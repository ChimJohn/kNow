package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
        this.gpsClickListener = gpsClickListener;
        this.viewPager2 = viewPager2;
        this.autoScrollHandler = new Handler(Looper.getMainLooper());
        setHasStableIds(true);
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
//        autoScrollHandler.removeCallbacks(autoScrollRunnable);
//        autoScrollRunnable = () -> {
//            int nextItem = viewPager2.getCurrentItem() + 1;
//            if (nextItem < getItemCount()) {
//                viewPager2.setCurrentItem(nextItem, true);
//            } else {
//            }
//        };
        holder.prepareAndPlayVideo();
//        autoScrollHandler.postDelayed(autoScrollRunnable, 4000);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull StoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.releasePlayer(); // Add this line
    }

    public void removeAutoScrollCallbacks() {
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacksAndMessages(null);
        }
    }
    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final PlayerView playerView;
        private final TextView storyCaption;
        private final Button gpsButton;
        private ExoPlayer exoPlayer;
        private final ProgressBar imageLoader;
        private ProgressBar progressBar;
        private Story story;

        public StoryViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.story_image);
            playerView = itemView.findViewById(R.id.player_view);
            storyCaption = itemView.findViewById(R.id.story_snippet);
            gpsButton = itemView.findViewById(R.id.btnGps);
            imageLoader = itemView.findViewById(R.id.image_loader);
            progressBar = itemView.findViewById(R.id.storyProgressBar);
        }

        public void bind(Story story, OnGpsClickListener gpsClickListener) {
            this.story = story;
            progressBar.setProgress(0);
            storyCaption.setText(story.getCaption());
            gpsButton.setOnClickListener(v -> {
                if (gpsClickListener != null) {
                    gpsClickListener.onGpsClick(story.getLatitude(), story.getLongitude());
                }
            });

//            if (story.isVideo()) {
//                imageView.setVisibility(View.INVISIBLE);
//                imageLoader.setVisibility(View.VISIBLE);
//                playerView.setVisibility(View.INVISIBLE);
//                if (exoPlayer == null) {
//                    exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
//                    playerView.setPlayer(exoPlayer);
//                }
//                MediaItem mediaItem = MediaItem.fromUri(story.getMediaUrl());
//                exoPlayer.setMediaItem(mediaItem);
//                exoPlayer.prepare();
//
//            } else {
            if (!story.isVideo()) {
                Log.d("WHYYY", "NOT VIDEO - " + story.getMediaUrl());
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
//            }
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
            Log.d("WHYYY MediaUrl:", String.valueOf(story.getMediaUrl()));
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e("WHYYY Error", "Player error: " + error.getMessage());
                    // Handle error (show message to user or retry)
                }
                @Override
                public void onPlaybackStateChanged(int state) {
                    Log.d("WHYYY State:", String.valueOf(state));
                    switch (state) {
                        case Player.STATE_BUFFERING:
                            Log.d("WHYYY", "Buffering...");
                            imageView.setVisibility(View.INVISIBLE);
                            imageLoader.setVisibility(View.VISIBLE);
                            playerView.setVisibility(View.GONE);
                            break;

                        case Player.STATE_READY:
                            long duration = exoPlayer.getDuration();
                            Log.d("WHYYY Duration:", String.valueOf(duration));

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

                        case Player.STATE_ENDED:
                            Log.d("WHYYY", "Playback ended.");
                            break;

                        case Player.STATE_IDLE:
                            Log.d("WHYYY", "Player is idle.");
                            break;

                        default:
                            Log.d("WHYYY", "Unknown state: " + state);
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
                    Log.d("WHYYY", bitmap.toString());
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
