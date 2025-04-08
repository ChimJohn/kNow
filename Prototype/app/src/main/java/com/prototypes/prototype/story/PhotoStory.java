package com.prototypes.prototype.story;

import android.content.Context;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PhotoStory extends Story{

    static public void preloadPhotos(Context context, ArrayList<Story> storyList) {
        if (storyList == null || storyList.isEmpty()) return;
        for (Story story : storyList) {
            if (!story.isVideo()) {
                Glide.with(context).load(story.getMediaUrl()).preload();
            }
        }
    }

}
