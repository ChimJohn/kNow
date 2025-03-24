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

import com.prototypes.prototype.R;

import java.util.ArrayList;
import java.util.List;

public class StoryViewFragment extends Fragment implements StoryViewAdapter.StoryListener{
    private ArrayList<Story> storyList;
    private ViewPager2 viewPager2;
    private int position;
    private StoryViewAdapter adapter;
    public static StoryViewFragment newInstance(List<Story> stories, int position) {
        StoryViewFragment fragment = new StoryViewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("stories", new ArrayList<>(stories));
        bundle.putInt("position", position); // Pass position to know which story to show
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyList = getArguments().getParcelableArrayList("stories");
            position = getArguments().getInt("position", 0);
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
        adapter = new StoryViewAdapter(getContext(), storyList, this);
        viewPager2.setAdapter(adapter);
    }
    @Override
    public void onStoryTap(int position) {
        // Handle the story tap event here
        Log.d("StoryViewFragment", "Tapped story at position: " + position);
        // Navigate to another screen or update UI
    }

}
