package com.prototypes.prototype.story;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

import java.util.ArrayList;
import java.util.List;

public class StoryViewFragment extends Fragment{
    private ArrayList<Story> storyList;
    private ViewPager2 viewPager2;
    private StoryViewAdapter adapter;
    public static StoryViewFragment newInstance(List<Story> stories) {
        StoryViewFragment fragment = new StoryViewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("stories", new ArrayList<>(stories));
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyList = getArguments().getParcelableArrayList("stories");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_story_view, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager2 = view.findViewById(R.id.storyViewPager); // RecyclerView to display the stories

        adapter = new StoryViewAdapter(getContext(), storyList);
        viewPager2.setAdapter(adapter);
        viewPager2.setOffscreenPageLimit(2); // Load 2 adjacent pages in memory
        preloadMedia();
    }

    private void preloadMedia() {
        if (storyList == null || storyList.isEmpty()) return;

        for (int i = 0; i < storyList.size(); i++) {
            Story story = storyList.get(i);
            if (!story.isVideo()) {
                // Preload image
                Glide.with(requireContext()).load(story.getMediaUrl()).preload();
            }
        }
    }

}
