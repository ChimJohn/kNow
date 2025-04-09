package com.prototypes.prototype.classes;

import android.content.Context;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PhotoStory extends Story {

    public PhotoStory(String id, String userId, String caption, String category, String mediaUrl, double latitude, double longitude, String mediaType, String thumbnailUrl) {
        super(id, userId, caption, category, mediaUrl, latitude, longitude, mediaType, thumbnailUrl);
    }

    static public void preloadPhotos(Context context, ArrayList<Story> storyList) {
        if (storyList == null || storyList.isEmpty()) return;
        for (Story story : storyList) {
            if (!story.isVideo()) {
                Glide.with(context).load(story.getMediaUrl()).preload();
            }
        }
    }

}
