package com.prototypes.prototype.story;

import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StoryViewAdapter extends FragmentStateAdapter {

    private final List<Story> stories; // List of stories

    public StoryViewAdapter(@NonNull Fragment fragment, List<Story> stories) {
        super(fragment);
        this.stories = stories;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Story story = stories.get(position);
        return StoryViewFragment.newInstance(story.getUserId(), story.getCaption(), story.getMediaUrl());
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }
}
